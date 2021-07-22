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

import java.io.InputStream
import java.nio.ByteBuffer

trait DeserialisationInputStream extends InputStream {

    val buff: ByteBuffer

    override def read(): Int = buff.getInt()

    override def read(b: Array[Byte]): Int = {
        buff.get(b)
        b.length
    }

    def readObject(): Any

    def readPrimitive(): AnyVal

    def readString(limit: Int = buff.limit()): String

    def readEnum[E <: Enum[E]](limit: Int = buff.limit()): E

    def readArray(): Array[Any]

    def readClass(): Class[_]


}

object DeserialisationInputStream {
    implicit def unwrap(in: DeserialisationInputStream): ByteBuffer = in.buff
}
