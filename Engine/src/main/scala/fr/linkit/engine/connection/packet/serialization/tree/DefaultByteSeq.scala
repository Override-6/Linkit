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

package fr.linkit.engine.connection.packet.serialization.tree

import fr.linkit.api.connection.packet.serialization.tree.ByteSeq
import fr.linkit.engine.local.mapping.ClassMappings
import fr.linkit.engine.local.utils.{NumberSerializer, ScalaUtils}

case class DefaultByteSeq(override val array: Array[Byte]) extends ByteSeq {

    lazy val clazz: Option[Class[_]] = findClassAt(0)

    override def findClass[T]: Option[Class[_]] = clazz

    override def getClassOfSeq[T]: Class[T] = clazz match {
        case None        => throw new NoSuchElementException(s"Header class not found into byte array ('${ScalaUtils.toPresentableString(array.drop(4))}').")
        case Some(value) => value match {
            case clazz: Class[T] => clazz
        }
    }

    override def isClassDefined: Boolean = clazz.isDefined

    override def apply(i: Int): Byte = array(i)

    override def classExists(f: Class[_] => Boolean): Boolean = clazz exists f

    override def sameFlagAt(pos: Int, flag: Byte): Boolean = array.length > pos && array(pos) == flag

    private def findClassAt(index: Int): Option[Class[_]] = {
        if (array.length - index < 4)
            None
        else {
            val i = NumberSerializer.deserializeInt(array, index)
            //println(s"index = ${index}")
            //println(s"i = ${i}")
            val v = ClassMappings.findClass(i)
            //println(s"v = ${v}")
            v
        }
    }
}

object DefaultByteSeq {

    implicit def unwrap(seq: DefaultByteSeq): Array[Byte] = seq.array
}
