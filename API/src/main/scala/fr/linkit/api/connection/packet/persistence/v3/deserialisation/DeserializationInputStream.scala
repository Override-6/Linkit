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

package fr.linkit.api.connection.packet.persistence.v3.deserialisation

import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext

import java.io.InputStream
import java.nio.ByteBuffer

trait DeserializationInputStream extends InputStream {

    val progression: DeserializationProgression
    val buff: ByteBuffer
    val context: PacketPersistenceContext

    override def read(): Int = buff.getInt()

    override def read(b: Array[Byte]): Int = {
        buff.get(b)
        b.length
    }

    def readObject[A](): A

    def readPrimitive(): AnyVal

    def readArray[A](): Array[A]

    def readString(limit: Int = buff.limit()): String

    def readEnum(limit: Int = buff.limit(), hint: Class[_] = null): Enum[_]

    def readClass(): Class[_]


}

object DeserializationInputStream {
    implicit def unwrap(in: DeserializationInputStream): ByteBuffer = in.buff
}
