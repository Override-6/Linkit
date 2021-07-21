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

package fr.linkit.engine.connection.packet.persistence.v3.serialisation

import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialOutputStream
import fr.linkit.engine.connection.packet.persistence.tree.DefaultSerialContext.ByteHelper
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialOutputStream._
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer

class DefaultSerialOutputStream(override val buff: ByteBuffer, context: PersistenceContext) extends SerialOutputStream {

    override def writeObject(obj: Any): Unit = {
        context.getNode(obj).writeBytes(this)
    }

    override def writePrimitive(anyVal: AnyVal): Unit = {
        val (bytes, flag) = anyVal match {
            case i: Int     => (NumberSerializer.serializeNumber(i, true), IntFlag)
            case b: Byte    => (NumberSerializer.serializeNumber(b, true), ByteFlag)
            case s: Short   => (NumberSerializer.serializeNumber(s, true), ShortFlag)
            case l: Long    => (NumberSerializer.serializeNumber(l, true), LongFlag)
            case d: Double  => (NumberSerializer.serializeNumber(java.lang.Double.doubleToLongBits(d), true), DoubleFlag)
            case f: Float   => (NumberSerializer.serializeNumber(java.lang.Float.floatToIntBits(f), true), FloatFlag)
            case b: Boolean => ((1: Byte) /\ (if (b) 1 else 0).toByte, BooleanFlag)
            case c: Char    => (NumberSerializer.serializeNumber(c.toInt, true), CharFlag)
        }
        buff.put(flag).put(bytes)
    }

    override def writeString(str: String): Unit = StringFlag +: str.getBytes()

    override def writeArray(array: Array[Any]): Unit = ???

}

object DefaultSerialOutputStream {

    val ByteFlag   : Byte = 1
    val ShortFlag  : Byte = 2
    val IntFlag    : Byte = 4
    val LongFlag   : Byte = 8
    val FloatFlag  : Byte = 16
    val DoubleFlag : Byte = 32
    val CharFlag   : Byte = 64
    val BooleanFlag: Byte = 127

    val StringFlag: Byte = -101
}
