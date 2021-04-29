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

package fr.linkit.core.connection.packet.serialization.tree

import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer

import java.sql.Timestamp

case class ByteSeqInfo(bytes: Array[Byte]) {

    lazy val classType: Option[Class[_]] = findClassAt(0)

    def isClassDefined: Boolean = classType.isDefined

    def findClassAt(index: Int): Option[Class[_]] = {
        if (bytes.length - index < 4)
            None
        else {
            val i = NumberSerializer.deserializeInt(bytes, index)
            println(s"index = ${index}")
            println(s"i = ${i}")
            println(s"classOf[Timestamp].getClass.getName.hashCode = ${classOf[Timestamp].getName.hashCode}")
            val v = ClassMappings.getClassOpt(NumberSerializer.deserializeInt(bytes, index))
            println(s"v = ${v}")
            v
        }
    }

    def apply(i: Int): Byte = bytes(i)

    def classExists(f: Class[_] => Boolean): Boolean = classType exists f

    def classExists(i: Int, f: Class[_] => Boolean): Boolean = findClassAt(i) exists f

    def sameFlag(flag: Byte): Boolean = bytes.nonEmpty && bytes(0) == flag
}
