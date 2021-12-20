/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.gnom.cache.sync.invokation.local.AbstractMethodInvocationException
import fr.linkit.api.gnom.cache.sync.tree.SyncObjectReference
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncClassRectifier.{SuperMethodModifiers, getMethodDescriptor}
import javassist._
import javassist.bytecode.MethodInfo

import java.lang.reflect.{Method, Modifier}
import scala.collection.mutable.ListBuffer

class SyncClassRectifier(desc: SyncStructureDescription[_],
                         syncClassName: String,
                         classLoader: GeneratedClassLoader,
                         superClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(syncClassName)

    makeExtend()
    fixAllMethods()
    addAllConstructors()
    fixNetworkObjectInherance()

    lazy val rectifiedClass: (Array[Byte], Class[SynchronizedObject[_]]) = {
        val bc = ctClass.toBytecode

        (bc, classLoader.defineClass(bc, ctClass.getName).asInstanceOf[Class[SynchronizedObject[_]]])
    }

    private def makeExtend(): Unit = {
        val ctSuperClass = pool.get(superClass.getName)
        if (superClass.isInterface)
            ctClass.addInterface(ctSuperClass)
        else
            ctClass.setSuperclass(ctSuperClass)
    }

    private def fixNetworkObjectInherance(): Unit = {
        if (!classOf[NetworkObject[_]].isAssignableFrom(superClass))
            return

        def slashDot(cl: Class[_]): String = cl.getName.replace(".", "/")

        // Removes a potential 'reference' method that overrides the actual superClass's reference method and returns the wrong reference object
        val met = ctClass.getDeclaredMethods
                .find(m => m.getName == "reference" && m.getReturnType.getName != classOf[SyncObjectReference].getName)
                .get
        ctClass.removeMethod(met)
        addMethod("reference", s"()L${slashDot(classOf[NetworkObjectReference])};")
                .setBody("return location();")
    }

    private def addMethod(name: String, signature: String): CtMethod = {
        val info   = new MethodInfo(ctClass.getClassFile.getConstPool, name, signature)
        val method = CtMethod.make(info, ctClass)
        method.setModifiers(Modifier.PUBLIC)
        ctClass.addMethod(method)
        method
    }

    private def addAllConstructors(): Unit = {
        superClass.getDeclaredConstructors.foreach(constructor => if (!Modifier.isPrivate(constructor.getModifiers)) {
            if (constructor.getParameterCount > 0) {
                val params = constructor.getParameterTypes
                val const  = addConstructor(ctClass, params)
                const.setBody(
                    s"""
                       |super(${params.indices.map(i => s"$$${i + 1}").mkString(",")});
                       |""".stripMargin)
            }
        })
    }

    private def addConstructor(target: CtClass, params: Array[Class[_]]): CtConstructor = {
        val ctConstructor = new CtConstructor(Array.empty, target)
        params.foreach(param => ctConstructor.addParameter(pool.get(param.getName)))
        target.addConstructor(ctConstructor)
        ctConstructor
    }

    private def fixAllMethods(): Unit = {
        implicit def extractModifiers(m: Method): Int = m.getModifiers

        val methodDescs      = desc.listMethods()
        val superDescriptors = ListBuffer.empty[String]
        val methodNames      = ListBuffer.empty[String]

        def fixMethod(desc: MethodDescription): Unit = {
            val javaMethod   = desc.javaMethod
            val name         = javaMethod.getName
            val superfunName = s"super$$$name$$"
            val superfunInfo = new MethodInfo(ctClass.getClassFile.getConstPool, superfunName, getMethodDescriptor(javaMethod))
            if (superDescriptors.contains(superfunName + superfunInfo.getDescriptor))
                return
            val anonfun  = getAnonFun(desc)
            val superfun = CtMethod.make(superfunInfo, ctClass)
            superfun.setModifiers(SuperMethodModifiers)

            ctClass.addMethod(superfun)
            superfun.setBody(getSuperFunBody(javaMethod))
            val body = getAnonFunBody(javaMethod, superfun)
            anonfun.setBody(body)

            superDescriptors += superfunName + superfunInfo.getDescriptor
            methodNames += javaMethod.getName
        }

        for (desc <- methodDescs) {
            fixMethod(desc)
        }
    }

    private def getAnonFun(desc: MethodDescription): CtMethod = {
        val javaMethod       = desc.javaMethod
        val methodReturnType = javaMethod.getReturnType
        val methodDesc       = getMethodDescriptor(javaMethod)
        val anonFunPrefix    = s"$$anonfun$$${javaMethod.getName}$$"
        val filtered         = ctClass.getDeclaredMethods
                .filter(_.getName.startsWith(anonFunPrefix))
                .filterNot(_.getName.endsWith("adapted"))
        val method           = filtered
                .find { x =>
                    val params = x.getParameterTypes.drop(1).dropRight(1)
                    val desc   = getMethodDescriptor(params, methodReturnType)
                    desc == methodDesc
                }
                .getOrElse {
                    throw new NoSuchElementException(s"Could not find anonymous function '$anonFunPrefix'")
                }
        method
    }

    private def getAnonFunBody(javaMethod: Method, superFun: CtMethod): String = {

        val params     = javaMethod.getParameterTypes
        val arrayIndex = params.length + 2
        val str        = s"$$1.${superFun.getName}(${
            (0 until javaMethod.getParameterCount).map(i => {
                val clazz = params(i)
                s"(${clazz.getTypeName}) ${getWrapperFor(clazz, s"$$$arrayIndex[$i]", true)}"
            }).mkString(",")
        })"

        if (javaMethod.getReturnType == Void.TYPE)
            s"{$str; return null;}"
        else {
            s"{return ${getWrapperFor(superFun.getReturnType, str)};}"
        }
    }

    private def getWrapperFor(returnType: CtClass, str: String): String = {
        if (returnType.isPrimitive) {
            var wrapperName = returnType.getName.head.toUpper + returnType.getName.drop(1)
            if (wrapperName == "Int")
                wrapperName = "Integer"
            s"$wrapperName.valueOf($str)"
        }
        else str
    }

    private def getWrapperFor(returnType: Class[_], str: String, unwrap: Boolean = false): String = {
        if (returnType.isPrimitive) {
            var wrapperName = returnType.getName.head.toUpper + returnType.getName.drop(1)
            if (wrapperName == "Int")
                wrapperName = "Integer"
            if (unwrap) {
                val methodName = returnType.getName + "Value"
                s"(($wrapperName) $str).$methodName()"
            } else
                s"$wrapperName.valueOf($str)"
        }
        else str
    }

    private def getSuperFunBody(javaMethod: Method): String = {
        if (Modifier.isAbstract(javaMethod.getModifiers)) {
            s"""{
               |Throwable x = new ${classOf[AbstractMethodInvocationException].getName}(\"Attempted to call an abstract method on this object. (${javaMethod.getName} in ${superClass} is abstract.)\", null);
               |throw x;
               |}
               |""".stripMargin
        } else {
            val str = {
                s"super.${javaMethod.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$$i").mkString(",")});".stripMargin
            }
            if (javaMethod.getReturnType == Void.TYPE) str
            else s"return $str"
        }
    }

}

object SyncClassRectifier {

    //used AccessFlags that are not in the java's reflection public api
    val Access_Synthetic          = 0x00001000
    val SuperMethodModifiers: Int = Modifier.PRIVATE + Access_Synthetic

    import java.{lang => l}

    val StringToPrimitiveClass =
        Map(
            "int" -> Integer.TYPE,
            "double" -> l.Double.TYPE,
            "float" -> l.Float.TYPE,
            "char" -> l.Character.TYPE,
            "boolean" -> l.Boolean.TYPE,
            "long" -> l.Boolean.TYPE,
            "void" -> l.Void.TYPE,
            "short" -> l.Short.TYPE,
            "byte" -> l.Byte.TYPE
        )

    val StringToPrimitiveID =
        Map(
            "int" -> "I",
            "double" -> "D",
            "float" -> "F",
            "char" -> "C",
            "boolean" -> "Z",
            "long" -> "J",
            "void" -> "V",
            "short" -> "S",
            "byte" -> "B"
        )

    def getMethodDescriptor(method: Method): String = {
        getMethodDescriptor(method.getParameterTypes, method.getReturnType)
    }

    def getMethodDescriptor(params: Array[Class[_]], returnType: Class[_]): String = {

        val sb = new StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringClass(clazz))
        }
        sb.append(')')
                .append(typeStringClass(returnType))
        sb.toString()
    }

    def getMethodDescriptor(params: Array[CtClass], returnType: Class[_]): String = {

        val sb = new StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringCtClass(clazz))
        }
        sb.append(')')
                .append(typeStringClass(returnType))
        sb.toString()
    }

    def typeStringCtClass(clazz: CtClass): String = {
        var cl      = clazz
        val finalSB = new StringBuilder
        while (cl.isArray) {
            finalSB.append("[")
            cl = clazz.getComponentType
        }
        val jvmTpe = StringToPrimitiveID.getOrElse(cl.getName, {
            val objSB = new StringBuilder()
            objSB.append("L")
                    .append(cl.getName.replace(".", "/"))
                    .append(";")
            objSB.toString()
        })
        finalSB.append(jvmTpe)
        finalSB.toString()
    }

    def typeStringClass(clazz: Class[_]): String = {
        if (clazz == Void.TYPE)
            return "V"
        val arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString
        arrayString.slice(1, arrayString.indexOf('@')).replace(".", "/")
    }
}
