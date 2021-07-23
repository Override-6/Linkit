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

import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserialisationInputStream
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.local.mapping.{ClassMappings, ClassNotMappedException}
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer

class DefaultDeserialisationInputStream(override val buff: ByteBuffer,
                                        override val context: PersistenceContext) extends DeserialisationInputStream {

    val progress = new DefaultDeserialisationProgression(this)
    progress.initPool()

    override def readObject(): Any = {
        context.getDeserializationNode(this, progress).getObject(this)
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
        val array = new Array[Byte](limit - buff.position())
        buff.get(array)
        new String(array)
    }

    override def readArray(): Array[Any] = {
        checkFlag(ArrayFlag, "Array")
        ArrayPersistence.deserialize(this, progress).getObject(this).asInstanceOf[Array[Any]]
    }

    override def readEnum[E <: Enum[E]](limit: Int): E = {
        val enumType = readClass()
        val name     = readString(limit)
        Enum.valueOf[E](enumType.asInstanceOf[Class[E]], name)
    }

    override def readClass(): Class[_] = {
        val clazz = ClassMappings.getClass(buff.getInt())
        if (clazz == null)
            throw new ClassNotMappedException(s"No class code found at buffer position ${buff.position() - 4}")
        clazz
    }

    private def checkFlag(flag: Byte, flagName: String): Unit = {
        if (buff.get != flag) {
            val array = buff.array().slice(buff.position() - 1, buff.limit())
            throw MalFormedPacketException(array, s"Expected $flagName flag at start of $flagName expression.")
        }
    }

}
