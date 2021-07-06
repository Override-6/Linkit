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

import java.lang.reflect.{Method, Modifier}

class ClassRectifier(desc: PuppetDescription[_], puppetClassName: String,
                     classLoader: GeneratedClassClassLoader, expectedSuperClass: Class[_]) {

    private val pool = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(puppetClassName)
    ctClass.setSuperclass(pool.get(expectedSuperClass.getName))
    addAllSuperMethods()

    lazy val rectifiedClass: Class[_] = {
        ctClass.toClass(classLoader, expectedSuperClass.getProtectionDomain)
    }

    private def addAllSuperMethods(): Unit = {
        val methodDescs = desc.listMethods()
        for (desc <- methodDescs) {
            val javaMethod   = desc.method
            val name         = javaMethod.getName
            val superfunName = s"super$$$name"
            val anonfunName  = "$anonfun$" + name + "$1"
            val superfunInfo = new MethodInfo(ctClass.getClassFile.getConstPool, superfunName, generateSuperFunDescriptor(javaMethod))
            val superfun     = CtMethod.make(superfunInfo, ctClass)
            val anonfun      = ctClass.getDeclaredMethod(anonfunName)

            ctClass.addMethod(superfun)
            superfun.setBody(getSuperFunBody(javaMethod))
            anonfun.setBody(getAnonFunBody(javaMethod, superfunName))
            anonfun.bo
        }
    }

    private def getAnonFunBody(javaMethod: Method, superFunName: String): String = {
        s"return $$1.$superFunName(${(1 to javaMethod.getParameterCount).map(i => s"$$${i + 1}").mkString(",")});"
    }

    private def getSuperFunBody(javaMethod: Method): String = valueReturnBody(javaMethod) {
        s"""
           |super.${javaMethod.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$$i").mkString(",")});
           |""".stripMargin
    }

    private def valueReturnBody(javaMethod: Method)(str: String): String = {
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
