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

import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode

import java.io.OutputStream
import java.nio.ByteBuffer

trait SerialisationOutputStream extends OutputStream {

    val progression: SerialisationProgression

    val buff: ByteBuffer

    override def write(b: Int): Unit = buff.putInt(b)

    override def write(b: Array[Byte]): Unit = buff.put(b)

    def writeClass(clazz: Class[_]): Unit

    def objectNode(obj: Any): SerializerNode

    def primitiveNode(anyVal: AnyVal): SerializerNode

    def stringNode(str: String): SerializerNode

    def arrayNode(array: Array[_]): SerializerNode

    def enumNode(enum: Enum[_]): SerializerNode

}

object SerialisationOutputStream {

    implicit def unwrap(stream: SerialisationOutputStream): ByteBuffer = stream.buff
}