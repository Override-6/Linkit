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

case class ByteSeq(array: Array[Byte]) {

    lazy val headerClass: Option[Class[_]] = findClassAt(0)

    def getHeaderClass[T]: Class[T] = headerClass match {
        case None        => throw new NoSuchElementException("Header class not found into byte array.")
        case Some(value) => value match {
            case clazz: Class[T] => clazz
            case _               => throw new IllegalArgumentException("Header class found into byte array mut mismatches from requested type.")
        }
    }

    def isClassDefined: Boolean = headerClass.isDefined

    def findClassAt(index: Int): Option[Class[_]] = {
        if (array.length - index < 4)
            None
        else {
            val i = NumberSerializer.deserializeInt(array, index)
            println(s"index = ${index}")
            println(s"i = ${i}")
            println(s"classOf[Timestamp].getClass.getName.hashCode = ${classOf[Timestamp].getName.hashCode}")
            val v = ClassMappings.getClassOpt(NumberSerializer.deserializeInt(array, index))
            println(s"v = ${v}")
            v
        }
    }

    def apply(i: Int): Byte = array(i)

    def classExists(f: Class[_] => Boolean): Boolean = headerClass exists f

    def classExists(i: Int, f: Class[_] => Boolean): Boolean = findClassAt(i) exists f

    def sameFlag(flag: Byte): Boolean = array.nonEmpty && array(0) == flag
}

object ByteSeq {

    implicit def unwrap(seq: ByteSeq): Array[Byte] = seq.array
}
