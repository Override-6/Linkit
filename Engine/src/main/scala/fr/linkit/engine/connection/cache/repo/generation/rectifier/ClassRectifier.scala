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
import javassist.{ClassPool, CtMethod, LoaderClassPath}

import java.lang.reflect.Modifier.{isNative, isProtected, isStatic}
import java.lang.reflect.{Method, Modifier}

class ClassRectifier(puppetClassName: String, classLoader: GeneratedClassClassLoader, superClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(puppetClassName)

    removeAllSuperClassFinalFlags()
    ctClass.setSuperclass(pool.get(superClass.getName))
    addAllSuperMethods()

    lazy val rectifiedClass: Class[_] = {
        ctClass.toClass(classLoader, superClass.getProtectionDomain)
    }

    private def removeAllSuperClassFinalFlags(): Unit = {
        implicit def extractModifiers(m: CtMethod): Int = m.getModifiers
        import Modifier._
        val superCtClass = pool.get(superClass.getName)
        superCtClass.setModifiers(PUBLIC)
        superCtClass.getMethods.filterNot(m => isStatic(m) || isNative(m) || isProtected(m)).foreach(_.setModifiers(PUBLIC))
        superCtClass.toClass(superClass.getClassLoader, superClass.getProtectionDomain)
    }

    private def addAllSuperMethods(): Unit = {
        implicit def extractModifiers(m: Method): Int = m.getModifiers
        val methodDescs = superClass.getMethods.filterNot(m => isStatic(m) || isNative(m) || isProtected(m))
        for (javaMethod <- methodDescs) {
            val name         = javaMethod.getName
            val superfunName = s"super$$$name"
            val anonfunName  = "$anonfun$" + name + "$1"
            val superfunInfo = new MethodInfo(ctClass.getClassFile.getConstPool, superfunName, generateSuperFunDescriptor(javaMethod))
            val superfun     = CtMethod.make(superfunInfo, ctClass)
            val anonfun      = ctClass.getDeclaredMethod(anonfunName)

            ctClass.addMethod(superfun)
            superfun.setBody(getSuperFunBody(javaMethod))
            anonfun.setBody(getAnonFunBody(javaMethod, superfunName))
        }
    }

    private def getAnonFunBody(javaMethod: Method, superFunName: String): String = {
        val str = s"$$1.$superFunName(${(1 to javaMethod.getParameterCount).map(i => s"$$${i + 1}").mkString(",")});"
        if (javaMethod.getReturnType == Void.TYPE)
            "{" + str + "return null;}"
        else s"{return $str}"
    }

    private def getSuperFunBody(javaMethod: Method): String = {
        val str = s"""
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
