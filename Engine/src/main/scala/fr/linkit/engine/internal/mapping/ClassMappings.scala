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

import fr.linkit.api.internal.system.AppLogger

import java.security.CodeSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ClassMappings {

    type ClassMappings = ClassMappings.type

    private val primitives   = mapPrimitives()
    private val classes      = mutable.HashMap.empty[Int, (Class[_], MappedClassInfo)]
    private val classLoaders = mutable.HashSet.empty[ClassLoader]
    private val sources      = mutable.HashSet.empty[CodeSource]

    private val listeners     = ListBuffer.empty[ClassMappingsListener]
    private var haveListeners = false

    private[mapping] def classCodes: Array[Int] = classes.keys.toArray

    def putClass(className: String, loader: ClassLoader): Unit = {
        //println(s"Class put ! ($className) of hash code ${className.hashCode}")
        val clazz     = Class.forName(className, false, loader)
        val classCode = className.hashCode
        classes.put(classCode, createMapValue(clazz))
        notifyListeners(classCode)
        MappedClassesTree.addClass(clazz)
    }

    def putClass(className: String): Unit = {
        for (loader <- classLoaders) try {
            putClass(className, loader)
            return
        } catch {
            case _: ClassNotFoundException =>
        }
        throw new ClassNotFoundException(className)
    }

    def addClassPath(source: CodeSource): Unit = sources += source

    def getClassPaths: List[CodeSource] = sources.toList

    def putClass(clazz: Class[_]): Unit = {
        val classCode = clazz.getName.hashCode
        classes.put(classCode, (clazz, getClassInfo(clazz)))
        notifyListeners(classCode)
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

    @inline
    def isRegistered(clazz: Class[_]): Boolean = isRegistered(clazz.getName)

    @inline
    def isRegistered(className: String): Boolean = classes.contains(className.hashCode)

    @inline
    def codeOfClass(clazz: Class[_]): Int = {
        val name = clazz.getName
        if (!classes.contains(name.hashCode)) {
            AppLogger.warn(s"Class map did not contained $clazz. (code: ${name.hashCode})")
            putClass(clazz)
        }
        name.hashCode
    }

    def findKnownClassInfo(code: Int): Option[MappedClassInfo] = {
        classes.get(code).map(_._2)
    }

    def getClassInfo(clazz: Class[_]): MappedClassInfo = {
        if ((clazz eq classOf[Object]) || clazz == null)
            return MappedClassInfo.Object
        classes.getOrElseUpdate(clazz.getName.hashCode, createMapValue(clazz))._2
    }

    def addListener(listener: ClassMappingsListener) = {
        listeners += listener
        haveListeners = true
    }

    private def createMapValue(clazz: Class[_]): (Class[_], MappedClassInfo) = {
        if ((clazz eq classOf[Object]) || clazz == null)
            return (clazz, MappedClassInfo.Object)
        classLoaders += clazz.getClassLoader
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

    private def notifyListeners(classCode: Int): Unit = {
        if (haveListeners)
            listeners.foreach(_.onClassMapped(classCode))
    }

}
