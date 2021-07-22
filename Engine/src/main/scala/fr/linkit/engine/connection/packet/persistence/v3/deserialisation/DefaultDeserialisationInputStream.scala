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
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.local.mapping.ClassMappings
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer

class DefaultDeserialisationInputStream(override val buff: ByteBuffer,
                                        context: PersistenceContext) extends DeserialisationInputStream {

    val progress = new DefaultDeserialisationProgression(this)

    override def readObject(): Any = {
        context.getDeserializationNode(this, progress).getObject(this)
    }

    override def readPrimitive(): AnyVal = {
        //println(s"Deserializing primitive number from bytes ${ScalaUtils.toPresentableString(bytes)}")
        //println(s"raw bytes = ${bytes.mkString("Array(", ", ", ")")}")
        import NumberSerializer.deserializeFlaggedNumber
        import fr.linkit.engine.local.utils.UnWrapper.unwrap
        lazy val value = deserializeFlaggedNumber[Long](this)
        //println(s"value = ${value}")
        val result = buff.get match {
            case IntFlag     => unwrap(value, _.intValue)
            case LongFlag    => unwrap(value, _.longValue)
            case BooleanFlag => unwrap(value, _.booleanValue)
            case FloatFlag   => unwrap(value, _ => java.lang.Float.intBitsToFloat(value.toInt))
            case DoubleFlag  => unwrap(value, _ => java.lang.Double.longBitsToDouble(value))
            case ByteFlag    => unwrap(value, _.byteValue)
            case ShortFlag   => unwrap(value, _.shortValue)
            case CharFlag    => unwrap(value, _.charValue)
        }
        result
    }

    override def readString(limit: Int): String = {
        val array = new Array[Byte](buff.position() - limit)
        buff.get(array)
        new String(array)
    }

    override def readArray(): Array[Any] = {
        ArrayPersistence.deserialize(this, progress, context).getObject(this).asInstanceOf[Array[Any]]
    }

    override def readEnum[E <: Enum[E]](limit: Int): E = {
        val enumType = readClass()
        val name     = readString(limit)
        Enum.valueOf[E](enumType.asInstanceOf[Class[E]], name)
    }

    override def readClass(): Class[_] = {
        ClassMappings.getClass(buff.getInt())
    }

}
