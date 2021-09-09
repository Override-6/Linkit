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

import java.nio.ByteBuffer

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.pool.{PacketObjectPool, PoolChunk}
import fr.linkit.engine.connection.packet.persistence.serializor.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.write.PacketObjectPool.{PacketObject, PoolObject}
import fr.linkit.engine.local.mapping.ClassMappings

class ObjectPoolWriter(config: PacketConfig, context: PersistenceContext, val buff: ByteBuffer) {

    private val pool = new PacketObjectPool(config, context)

    def writeRootObjects(roots: Array[AnyRef]): Unit = {
        val pool = this.pool
        for (o <- roots) { //registering all objects, and contained objects in the constant pool.
            pool.addObject(o)
        }
        var i: Byte = 0
        val chunks  = pool.getChunks
        //One chunk per type of data so chunk can't be over 127 as there is only 12 types that are handled
        //see ConstantProtocol and PacketObjectPool
        val len     = chunks.length.toByte
        while (i < len) {
            val chunk = chunks(i)
            if (chunk.size > 0)
                writeChunk(i, chunk)
            i += 1
        }
    }

    private def writeChunk(flag: Byte, poolChunk: PoolChunk[Any]): Unit = {
        val size = poolChunk.size
        //Write Chunk type and size
        buff.put(flag)
        buff.putChar(size)
        //Write content
        if (flag <= Int && flag > Char || (flag eq ContextRef)) {
            ArrayPersistence.writePrimitiveArrayContent(this, poolChunk.array, 0, poolChunk.size)
            return
        }

        @inline def foreach[T](@inline action: T => Unit): Unit = {
            val items = poolChunk.array.asInstanceOf[Array[T]]
            var i     = 0
            while (i < size) {
                action(items(i))
                i += 1
            }
        }

        flag match {
            case Class  => foreach[Class[_]](cl => buff.putInt(ClassMappings.codeOfClass(cl)))
            case String => foreach[String](putString)
            case Array  => foreach[Array[Any]](xs => ArrayPersistence.writeArray(this, pool.getChunk(Class), xs))
            case Object => foreach[PacketObject](putObject)
        }
    }

    def getPool: PacketObjectPool = pool

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

    private def putString(str: String): Unit = {
        buff.putInt(str.length).put(str.getBytes())
    }

    private def putObject(poolObj: PacketObject): Unit = {
        buff.putChar(poolObj.typePoolIndex)
        ArrayPersistence.writeArrayContent(this, poolObj.decomposed.asInstanceOf[Array[AnyRef]])
    }

    def putPoolRef(obj: PoolObject[_], putTag: Boolean): Unit = {
        if (putTag)
            buff.put(PoolRef)
        buff.putChar(obj.poolIndex)
    }

    @inline
    def putPoolRef(obj: AnyRef, putTag: Boolean): Unit = {
        if (putTag)
            buff.put(PoolRef)
        buff.putChar(pool.indexOf(obj).toChar)
    }

    private def putContextRef(id: Int): Unit = {
        buff.put(ContextRef)
            .putInt(id)
    }

    private def flag(flag: Byte): ByteBuffer = {
        buff.put(flag)
    }

}
