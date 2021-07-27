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

package fr.linkit.engine.connection.packet.persistence.v3.deserialisation

import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationInputStream
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.local.mapping.{ClassMappings, ClassNotMappedException}
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer

class DefaultDeserializationInputStream(override val buff: ByteBuffer,
                                        override val context: PacketPersistenceContext) extends DeserializationInputStream {

    override val progression = new DefaultDeserializationProgression(this, context)
    progression.initPool()

    override def readObject[A](): A = {
        progression.getNextDeserializationNode
            .deserialize(this)
            .asInstanceOf[A]
    }

    override def readPrimitive(): AnyVal = {
        import NumberSerializer.deserializeFlaggedNumber
        import fr.linkit.engine.local.utils.UnWrapper.unwrap
        lazy val value = deserializeFlaggedNumber[Long](this)
        val result = buff.get match {
            case IntFlag     => unwrap(value, _.intValue)
            case LongFlag    => unwrap(value, _.longValue)
            case BooleanFlag => unwrap(value, _.booleanValue)
            case FloatFlag   => unwrap(value, _ => java.lang.Float.intBitsToFloat(value.toInt))
            case DoubleFlag  => unwrap(value, _ => java.lang.Double.longBitsToDouble(value))
            case ByteFlag    => unwrap(value, _.byteValue)
            case ShortFlag   => unwrap(value, _.shortValue)
            case CharFlag    => unwrap(value, _.charValue)
            case _           =>
                val array = buff.array().slice(buff.position() - 1, buff.limit())
                throw MalFormedPacketException(array, "Expected any number flag at start of number expression.")
        }
        result
    }

    override def readString(limit: Int): String = {
        checkFlag(StringFlag, "String")
        readString0(limit)
    }

    override def readArray[A](): Array[A] = {
        checkFlag(ArrayFlag, "Array")
        ArrayPersistence
            .deserialize(this)
            .deserialize(this)
            .asInstanceOf[Array[A]]
    }

    override def readEnum(limit: Int, hint: Class[_]): Enum[_] = {
        val enumType = if (hint == null) readClass() else hint
        val name     = readString0(limit)
        def casted[E](any: Any): E = any.asInstanceOf[E]
        casted(Enum.valueOf(casted(enumType), name))
    }

    override def readClass(): Class[_] = {
        val code = buff.getInt()
        val clazz = ClassMappings.getClass(code)
        if (clazz == null)
            throw new ClassNotMappedException(s"No class code found at buffer position ${buff.position() - 4} ($code)")
        clazz
    }

    private def readString0(limit: Int): String = {
        val array = new Array[Byte](limit - buff.position())
        buff.get(array)
        new String(array)
    }

    private def checkFlag(flag: Byte, flagName: String): Unit = {
        if (buff.get != flag) {
            val array = buff.array().slice(buff.position() - 1, buff.limit())
            throw MalFormedPacketException(array, s"Expected $flagName flag at start of $flagName expression.")
        }
    }

}
