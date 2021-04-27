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

package fr.linkit.core.local.mapping

import fr.linkit.api.connection.packet.serialization.Serializer

import java.io.OutputStream
import java.security.CodeSource
import scala.collection.mutable

object ClassMappings {

    private val classes = new mutable.HashMap[Int, (String, ClassLoader)]()
    private val sources = mutable.HashSet.empty[CodeSource]

    def putClass(className: String, loader: ClassLoader): Unit = {
        classes.put(className.hashCode, (className, loader))
        //println(s"Class put ! $className (${className.hashCode})")
    }

    def putSourceCode(source: CodeSource): Unit = {
        sources += source
    }

    def getSources: List[CodeSource] = sources.toList

    def putClass(clazz: Class[_]): Unit = putClass(clazz.getName, clazz.getClassLoader)

    def getClassName(hashCode: Int): String = classes(hashCode)._1

    def getClassOpt(hashCode: Int): Option[Class[_]] = {
        classes.get(hashCode).map(pair => {
            Class.forName(pair._1, false, pair._2)
        })
    }

    def getClass(hashCode: Int): Class[_] = {
        getClassOpt(hashCode).get
    }

    def getClassNameOpt(hashCode: Int): Option[String] = classes.get(hashCode).map(_._1)

    def isRegistered(hashCode: Int): Boolean = classes.contains(hashCode)

    def isRegistered(clazz: Class[_]): Boolean = isRegistered(clazz.getName)

    def isRegistered(className: String): Boolean = classes.contains(className.hashCode)

    def serialize(serializer: Serializer, out: OutputStream): Unit = {
        val bytes = serializer.serialize(classes, true)
        out.write(bytes)
    }

}
