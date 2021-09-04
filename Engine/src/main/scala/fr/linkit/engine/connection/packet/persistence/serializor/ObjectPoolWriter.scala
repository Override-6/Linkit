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

package fr.linkit.engine.connection.packet.persistence.serializor

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.local.mapping.ClassMappings

import java.lang
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

class ObjectPoolWriter(config: PacketConfig, context: PersistenceContext, val buff: ByteBuffer) {

    val headerIndex: Int = buff.position()
    buff.position(headerIndex + 2) //skip 2 bytes for the pool length
    private val objects        = new PacketConstantPool()
    private val waitingObjects = ListBuffer.empty[AnyRef]

    def writeRootObjects(objects: Array[AnyRef]): Unit = {
        for (o <- objects) {
            putAny(o)
            flushWaitingObjects()
        }
    }

    private def flushWaitingObjects(): Unit = {
        if (waitingObjects.isEmpty)
            return
        val clone = waitingObjects.clone()
        waitingObjects.clear()
        clone.foreach {
            case array: Array[Any] => putArray(array)
            case str: String       => putString(str)
            case ref: AnyRef       => putObject(ref)
        }
        flushWaitingObjects()
    }

    def writeHeaderSize(): Unit = {
        val size = objects.size
        if (size > lang.Short.MAX_VALUE * 2 + 1)
            throw new PacketPoolTooLongException(s"Packet pool size exceeds ${lang.Short.MAX_VALUE * 2 + 1}")
        buff.putChar(headerIndex, size.toChar) //write the pool length at the starting point.
    }

    def getPool: PacketConstantPool = objects

    private[serializor] def putAny(obj: Any): Unit = {
        obj match {
            case i: Int     => flag(Int).putInt(i)
            case b: Byte    => flag(Byte).put(b)
            case s: Short   => flag(Short).putShort(s)
            case l: Long    => flag(Long).putLong(l)
            case d: Double  => flag(Double).putDouble(d)
            case f: Float   => flag(Float).putFloat(f)
            case b: Boolean => flag(Boolean).put((if (b) 1 else 0): Byte)
            case c: Char    => flag(Char).putChar(c)

            case other: AnyRef => putNonPrimitive(other)
        }
    }

    //Non primitives can be stored in the pool
    private def putNonPrimitive(ref: AnyRef): Unit = {
        val oidx = objects.indexOf(ref)
        if (oidx >= 0) {
            putRef(oidx.toChar)
            return
        }
        val widx = waitingObjects.indexOf(ref)
        if (widx >= 0) {
            putRef((objects.size + widx).toChar)
            return
        }
        putRef((objects.size + waitingObjects.size).toChar)
    }

    private def putString(str: String): Unit = {
        //TODO Ensure that the same charset is used at the other side
        flag(String).putInt(str.length).put(str.getBytes())
    }

    def putArray(array: Array[Any]): Unit = {
        flag(Array)
        ArrayPersistence.writeArray(this, array)
    }

    private def putObject(obj: AnyRef): Unit = {
        val clazz   = obj.getClass
        val profile = config.getProfile[AnyRef](clazz, context)
        val code    = config.getReferencedCode(obj)
        if (code.isDefined) {
            buff.put(ContextRef).putInt(code.get)
            return
        }

        val array = profile.toArray(obj)
        flag(Object)
        buff.putInt(ClassMappings.codeOfClass(clazz))
        ArrayPersistence.writeArrayContent(this, array)
    }

    private def putRef(idx: Char): Unit = {
        buff.put(PoolRef)
                .putChar(idx)
    }

    private def flag(flag: Byte): ByteBuffer = {
        buff.put(flag)
    }
}
