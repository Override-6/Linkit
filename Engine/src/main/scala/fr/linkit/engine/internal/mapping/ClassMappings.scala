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

    type ClassMappings = ClassMappings.type

    private val primitives     = mapPrimitives()
    private val classes        = mutable.HashMap.empty[Int, (Class[_], MappedClassInfo)]
    private val unknownClasses = mutable.HashMap.empty[Int, MappedClassInfo]
    private val sources        = mutable.HashSet.empty[CodeSource]

    def putClass(className: String, loader: ClassLoader): Unit = {
        //println(s"Class put ! ($className) of hash code ${className.hashCode}")
        val clazz = Class.forName(className, false, loader)
        classes.put(className.hashCode, createMapValue(clazz))
    }

    def putUnknownClass(info: MappedClassInfo): Unit = {
        unknownClasses.put(info.classCode, info)
    }

    def addClassPath(source: CodeSource): Unit = sources += source

    def getClassPaths: List[CodeSource] = sources.toList

    def putClass(clazz: Class[_]): Unit = {
        classes.put(clazz.getName.hashCode, (clazz, getClassInfo(clazz)))
    }

    def findClass(hashCode: Int): Option[Class[_]] = {
        //println(s"Getting class from hashcode $hashCode")
        classes.get(hashCode).map(_._1).orElse(primitives.get(hashCode))
    }

    def getClass(hashCode: Int): Class[_] = {
        //println(s"Getting class from hashcode $hashCode")
        findClass(hashCode).orNull
    }

    def getClassNameOpt(hashCode: Int): Option[String] = classes.get(hashCode).map(_._1.getName)

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

    def findUnknownClassInfo(code: Int): Option[MappedClassInfo] = {
        unknownClasses.get(code)
    }

    def findKnownClassInfo(code: Int): Option[MappedClassInfo] = {
        classes.get(code).map(_._2)
    }

    def getClassInfo(clazz: Class[_]): MappedClassInfo = {
        if ((clazz eq classOf[Object]) || clazz == null)
            return MappedClassInfo.Object
        classes.getOrElseUpdate(clazz.getName.hashCode, createMapValue(clazz))._2
    }

    private def createMapValue(clazz: Class[_]): (Class[_], MappedClassInfo) = {
        if ((clazz eq classOf[Object]) || clazz == null)
            return (clazz, MappedClassInfo.Object)
        (clazz, MappedClassInfo(clazz))
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
