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

package fr.linkit.engine.gnom.cache.sync.generation.rectifier

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.description.{MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.engine.gnom.cache.sync.generation.rectifier.ClassRectifier.{StringToPrimitiveID, SuperMethodModifiers}
import javassist.bytecode.MethodInfo
import javassist.{ClassPool, CtClass, CtConstructor, CtMethod, LoaderClassPath}
import java.lang.reflect.{Method, Modifier}

import scala.collection.mutable.ListBuffer

class ClassRectifier(desc: SyncObjectSuperclassDescription[_], syncClassName: String, classLoader: GeneratedClassLoader, superClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(syncClassName)

    ctClass.setSuperclass(pool.get(superClass.getName))
    fixAllMethods()
    addAllConstructors()


    lazy val rectifiedClass: (Array[Byte], Class[SynchronizedObject[_]]) = {
        val bc = ctClass.toBytecode

        (bc, classLoader.defineClass(bc, ctClass.getName).asInstanceOf[Class[SynchronizedObject[_]]])
    }

    private def addAllConstructors(): Unit = {
        superClass.getDeclaredConstructors.foreach(constructor => if (!Modifier.isPrivate(constructor.getModifiers)) {
            if (constructor.getParameterCount > 0)
                addConstructor(constructor.getParameterTypes)
        })
    }

    private def addConstructor(params: Array[Class[_]]): Unit = {
        val ctConstructor = new CtConstructor(Array.empty, ctClass)
        params.foreach(param => ctConstructor.addParameter(pool.get(param.getName)))
        ctConstructor.setBody(
            s"""
               |super(${params.indices.map(i => s"$$${i + 1}").mkString(",")});
               |""".stripMargin)
        ctClass.addConstructor(ctConstructor)
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
        val str =
            s"""
               |super.${javaMethod.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$$i").mkString(",")});
               |""".stripMargin
        if (javaMethod.getReturnType == Void.TYPE)
            str
        else s"return $str"
    }

    private def getMethodDescriptor(method: Method): String = {
        getMethodDescriptor(method.getParameterTypes, method.getReturnType)
    }

    private def getMethodDescriptor(params: Array[Class[_]], returnType: Class[_]): String = {

        val sb = new StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringClass(clazz))
        }
        sb.append(')')
            .append(typeStringClass(returnType))
        sb.toString()
    }

    private def getMethodDescriptor(params: Array[CtClass], returnType: Class[_]): String = {

        val sb = new StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringCtClass(clazz))
        }
        sb.append(')')
            .append(typeStringClass(returnType))
        sb.toString()
    }

    private def typeStringCtClass(clazz: CtClass): String = {
        var cl = clazz
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

    private def typeStringClass(clazz: Class[_]): String = {
        if (clazz == Void.TYPE)
            return "V"
        val arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString
        arrayString.slice(1, arrayString.indexOf('@')).replace(".", "/")
    }
}

object ClassRectifier {

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
}
