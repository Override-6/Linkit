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

package fr.linkit.engine.internal.mapping

import java.security.CodeSource

import fr.linkit.api.internal.system.AppLogger

import scala.collection.mutable

object ClassMappings {

    private val primitives = mapPrimitives()
    private val classes    = new mutable.HashMap[Int, Class[_]]()
    private val sources    = mutable.HashSet.empty[CodeSource]

    def putClass(className: String, loader: ClassLoader): Unit = {
        //println(s"Class put ! ($className) of hash code ${className.hashCode}")
        val clazz = Class.forName(className, false, loader)
        classes.put(className.hashCode, clazz)
    }

    def addClassPath(source: CodeSource): Unit = sources += source

    def getClassPaths: List[CodeSource] = sources.toList

    def putClass(clazz: Class[_]): Unit = putClass(clazz.getName, clazz.getClassLoader)

    def getClassName(hashCode: Int): String = classes(hashCode).getName

    def findClass(hashCode: Int): Option[Class[_]] = {
        //println(s"Getting class from hashcode $hashCode")
        classes.get(hashCode).orElse(primitives.get(hashCode))
    }

    def getClass(hashCode: Int): Class[_] = {
        //println(s"Getting class from hashcode $hashCode")
        findClass(hashCode).orNull
    }

    def getClassNameOpt(hashCode: Int): Option[String] = classes.get(hashCode).map(_.getName)

    def isRegistered(hashCode: Int): Boolean = classes.contains(hashCode)

    @inline def isRegistered(clazz: Class[_]): Boolean = isRegistered(clazz.getName)

    @inline def isRegistered(className: String): Boolean = classes.contains(className.hashCode)

    @inline def codeOfClass(clazz: Class[_]): Int = {
        val name = clazz.getName
        if (!classes.contains(name.hashCode)) {
            AppLogger.warn(s"Class map did not contained $clazz. (code: ${name.hashCode})")
            putClass(clazz)
        }
        name.hashCode
    }

    private def mapPrimitives(): Map[Int, Class[_]] = {
        import java.{lang => l}
        Array(
            Integer.TYPE,
            l.Byte.TYPE,
            l.Short.TYPE,
            l.Long.TYPE,
            l.Double.TYPE,
            l.Float.TYPE,
            l.Boolean.TYPE,
            Character.TYPE
        ).map(cl => (cl.getName.hashCode, cl))
            .toMap
    }

}
