/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj.generation.rectifier

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.description.{MethodDescription, SyncObjectSuperClassDescription}
import fr.linkit.api.connection.cache.obj.generation.GeneratedClassLoader
import fr.linkit.engine.connection.cache.obj.generation.rectifier.ClassRectifier.{StringToPrimitiveID, SuperMethodModifiers}
import javassist.bytecode.MethodInfo
import javassist.{ClassPool, CtClass, CtMethod, LoaderClassPath}

import java.lang.reflect.{Method, Modifier}
import scala.collection.mutable.ListBuffer

class ClassRectifier(desc: SyncObjectSuperClassDescription[_], puppetClassName: String, classLoader: GeneratedClassLoader, superClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(puppetClassName)

    //removeAllSuperClassFinalFlags()
    ctClass.setSuperclass(pool.get(superClass.getName))
    fixAllMethods()

    lazy val rectifiedClass: (Array[Byte], Class[SynchronizedObject[_]]) = {
        (ctClass.toBytecode, ctClass.toClass(classLoader, null).asInstanceOf[Class[SynchronizedObject[_]]])
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
                .get
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
        val name = clazz.getName
        StringToPrimitiveID.getOrElse(name, {
            var cl = clazz
            val sb = new StringBuilder
            while (cl.isArray) {
                sb.append("[")
                cl = clazz.getComponentType
            }
            sb.append("L")
                    .append(cl.getName.replace(".", "/"))
                    .append(";")
            sb.toString()
        })
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
