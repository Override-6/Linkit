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

package fr.linkit.engine.connection.cache.repo.generation.rectifier

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import javassist.bytecode.MethodInfo
import javassist.{ClassPool, CtClass, CtMethod, LoaderClassPath}

import java.lang.reflect.{Method, Modifier}

class ClassRectifier(desc: PuppetDescription[_], puppetClassName: String, classLoader: GeneratedClassClassLoader, superClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(puppetClassName)

    //FIXME removeAllSuperClassFinalFlags()
    ctClass.setSuperclass(pool.get(superClass.getName))
    fixAllMethods()

    lazy val rectifiedClass: (Array[Byte], Class[_]) = {
        (ctClass.toBytecode, ctClass.toClass(classLoader, superClass.getProtectionDomain))
    }

    private def removeAllSuperClassFinalFlags(): Unit = {
        implicit def extractModifiers(m: CtMethod): Int = m.getModifiers
        import Modifier._
        val superCtClass = pool.get(superClass.getName)
        superCtClass.setModifiers(PUBLIC)
        superCtClass.getMethods.filterNot(m => isStatic(m) || isNative(m) || isProtected(m)).foreach(_.setModifiers(PUBLIC))
        superCtClass.toClass(superClass.getClassLoader, superClass.getProtectionDomain)
    }

    private def fixAllMethods(): Unit = {
        implicit def extractModifiers(m: Method): Int = m.getModifiers

        val methodDescs = desc.listMethods()
        for (descs <- methodDescs) {
            val javaMethod   = descs.javaMethod
            val name         = javaMethod.getName
            val superfunName = s"super$$$name"
            val superfunInfo = new MethodInfo(ctClass.getClassFile.getConstPool, superfunName, generateSuperFunDescriptor(javaMethod))
            val superfun     = CtMethod.make(superfunInfo, ctClass)
            val anonfun      = getAnonFun(javaMethod)

            ctClass.addMethod(superfun)
            superfun.setBody(getSuperFunBody(javaMethod))
            anonfun.setBody(getAnonFunBody(javaMethod, superfun))
        }
    }

    private def getAnonFun(javaMethod: Method): CtMethod = {
        val argsStrings = javaMethod.getParameterTypes.map(cl => cl.getSimpleName)
        val v           = ctClass
                .getDeclaredMethods
                .filter(_.getName.startsWith("$anonfun$" + javaMethod.getName))
                .find(m => {
                    val v = m.getParameterTypes.drop(1).map(cl => cl.getSimpleName)
                    v sameElements argsStrings
                })
        v.get
    }

    private def getAnonFunBody(javaMethod: Method, superFun: CtMethod): String = {
        val str = s"$$1.${superFun.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$${i + 1}").mkString(",")})"
        if (javaMethod.getReturnType == Void.TYPE)
            "{" + str + ";return null;}"
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

    private def getSuperFunBody(javaMethod: Method): String = {
        val str =
            s"""
               |super.${javaMethod.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$$i").mkString(",")});
               |""".stripMargin
        if (javaMethod.getReturnType == Void.TYPE)
            str
        else s"return $str"
    }

    private def generateSuperFunDescriptor(method: Method): String = {
        def typeString(clazz: Class[_]): String = {
            if (clazz == Void.TYPE)
                return "V"
            val arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString
            arrayString.slice(1, arrayString.indexOf('@')).replace(".", "/")
        }

        val sb = new StringBuilder("(")
        method.getParameterTypes.foreach { clazz =>
            sb.append(typeString(clazz))
        }
        sb.append(')')
                .append(typeString(method.getReturnType))
        sb.toString()
    }
}

object ClassRectifier {

    //used AccessFlags that are not in the java's reflection public api
    val Access_Synthetic          = 0x00001000
    val SuperMethodModifiers: Int = Modifier.PRIVATE + Access_Synthetic
}