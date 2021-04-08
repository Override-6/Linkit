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
import java.util

object ClassMappings {

    private val classes = new util.HashMap[Int, String]()

    def putClass(className: String): Unit = {
        classes.put(className.hashCode, className)
        //println(s"Class put ! $className (${className.hashCode})")
    }

    def putClass(clazz: Class[_]): Unit = putClass(clazz.getName)

    def getClassName(hashCode: Int): String = classes.get(hashCode)

    def getClassNameOpt(hashCode: Int): Option[String] = Option(classes.get(hashCode))

    def serialize(serializer: Serializer, out: OutputStream): Unit = {
        val bytes = serializer.serialize(classes, true)
        out.write(bytes)
    }

}
