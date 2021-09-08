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

package fr.linkit.engine.connection.packet.persistence.serializor.write

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.write.PacketObjectPool.{ContextObject, PacketObject, PoolObject}
import fr.linkit.engine.connection.packet.persistence.serializor.{ArrayPersistence, PacketPoolTooLongException}
import fr.linkit.engine.local.mapping.ClassMappings

import java.lang
import java.nio.ByteBuffer

class ObjectPoolWriter(config: PacketConfig, context: PersistenceContext, val buff: ByteBuffer) {

    val headerIndex: Int = buff.position()
    buff.position(headerIndex + 2) //skip 2 bytes for the pool length
    private val objects = new PacketObjectPool(config, context)

    def writeRootObjects(roots: Array[AnyRef]): Unit = {
        val objects = this.objects
        for (o <- roots) { //registering all objects, and contained objects in the constant pool.
            objects.add(o)
        }
        var i   = 0
        val len = objects.size
        while (i < len) {
            objects(i) match {
                case wrapper: PacketObject  => putAny(wrapper)
                case wrapper: ContextObject => putContextRef(wrapper.refInt)
            }
            i += 1
        }
    }

    def writeHeaderSize(): Unit = {
        val size = objects.size
        if (size > lang.Short.MAX_VALUE * 2 + 1)
            throw new PacketPoolTooLongException(s"Packet pool size exceeds ${lang.Short.MAX_VALUE * 2 + 1}")
        buff.putChar(headerIndex, size.toChar) //write the pool length at the starting point.
    }

    def getPool: PacketObjectPool = objects

    /*def putRef(obj: Any): Unit = {
        obj match {
            case i: Int     => flag(Int).putInt(i)
            case b: Byte    => flag(Byte).put(b)
            case s: Short   => flag(Short).putShort(s)
            case l: Long    => flag(Long).putLong(l)
            case d: Double  => flag(Double).putDouble(d)
            case f: Float   => flag(Float).putFloat(f)
            case b: Boolean => flag(Boolean).put((if (b) 1 else 0): Byte)
            case c: Char    => flag(Char).putChar(c)

            case str: String       => putString(str)
            case array: Array[Any] => putArray(array)
            case ref: AnyRef       => putPoolRef(ref)
        }
    }*/

    private def putAny(ref: PoolObject): Unit = {
        ref.obj match {
            case i: Int     => flag(Int).putInt(i)
            case b: Byte    => flag(Byte).put(b)
            case s: Short   => flag(Short).putShort(s)
            case l: Long    => flag(Long).putLong(l)
            case d: Double  => flag(Double).putDouble(d)
            case f: Float   => flag(Float).putFloat(f)
            case b: Boolean => flag(Boolean).put((if (b) 1 else 0): Byte)
            case c: Char    => flag(Char).putChar(c)

            case str: String       => putString(str)
            case array: Array[Any] => putArray(array)
            case _                 => putObject(ref)
        }
    }

    private def putString(str: String): Unit = {
        //TODO Ensure that the same charset is used at the other side
        flag(String).putInt(str.length).put(str.getBytes())
    }

    def putArray(array: Array[Any]): Unit = {
        flag(Array)
        ArrayPersistence.writeArray(this, array)
    }

    private def putObject(poolObj: PoolObject): Unit = {
        val obj   = poolObj.obj
        val clazz = obj.getClass

        poolObj match {
            case c: ContextObject => putContextRef(c.refInt)
            case p: PacketObject  =>
                flag(Object)
                buff.putInt(ClassMappings.codeOfClass(clazz))
                ArrayPersistence.writeArrayContent(this, p.decomposed.asInstanceOf[Array[AnyRef]])
        }
    }

    def putPoolRef(obj: PoolObject, putTag: Boolean): Unit = {
        if (putTag)
            buff.put(PoolRef)
        buff.putChar(obj.poolIndex)
    }

    @inline
    def putPoolRef(obj: AnyRef, putTag: Boolean): Unit = {
        if (putTag)
            buff.put(PoolRef)
        buff.putChar(objects.indexOf(obj).toChar)
    }

    private def putContextRef(id: Int): Unit = {
        buff.put(ContextRef)
                .putInt(id)
    }

    private def flag(flag: Byte): ByteBuffer = {
        buff.put(flag)
    }

}
