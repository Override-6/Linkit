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

package fr.linkit.api.connection.packet.persistence.v3.serialisation

import java.io.OutputStream
import java.nio.ByteBuffer

trait SerialOutputStream extends OutputStream {

    val buff: ByteBuffer

    override def write(b: Int): Unit = buff.putInt(b)

    override def write(b: Array[Byte]): Unit = buff.put(b)

    def writeObject(obj: Any): Unit

    def writePrimitive(anyVal: AnyVal): Unit

    def writeString(str: String): Unit

    def writeArray(array: Array[Any]): Unit

}

object SerialOutputStream {

    implicit def unwrap(stream: SerialOutputStream): ByteBuffer = stream.buff
}
