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

package fr.linkit.engine.gnom.persistence.serializor.write

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.persistence.Freezable
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.persistence.pool.{PoolChunk, SimpleContextObject}
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.gnom.persistence.serializor.write.PacketWriter.{Sizes2B, Sizes4B}
import fr.linkit.engine.gnom.persistence.serializor.{ArrayPersistence, PacketPoolTooLongException}
import fr.linkit.engine.internal.mapping.ClassMappings

import java.nio.ByteBuffer
import scala.annotation.switch

class PacketWriter(config: PersistenceConfig, val buff: ByteBuffer) extends Freezable {

    private val widePacket = config.widePacket
    private val pool       = new SerializerPacketObjectPool(config, if (widePacket) Sizes4B else Sizes2B)

    def addObjects(roots: Array[AnyRef]): Unit = {
        val pool = this.pool
        //Registering all objects, and contained objects in the constant pool.
        for (o <- roots) {
            pool.addObject(o)
        }
    }

    override def freeze(): Unit = pool.freeze()

    override def isFrozen: Boolean = pool.isFrozen

    /**
     * Will writes the chunks contained in the pool
     * This is a terminal action:
     */
    def writePool(): Unit = {
        if (pool.isFrozen)
            throw new IllegalStateException("Pool is frozen.")
        pool.freeze() //Ensure that the pool is no more susceptible to be updated.

        //Informs the deserializer if we sent a wide packet or not.
        //This means that for wide packets, pool references index are ints, and
        //for "non wide packets", references index are unsigned shorts
        buff.put((if (widePacket) 1 else 0): Byte)
        //Write the content
        writeChunks()
    }

    @inline
    private def writeChunks(): Unit = {
        //println(s"Write chunks : ${buff.array().mkString(", ")}")
        val pos     = buff.position()
        val refSize = if (widePacket) 4 else 2
        buff.position(pos + ChunkCount * refSize)

        var i        : Byte = 0
        val chunks          = pool.getChunks
        val len             = chunks.length.toByte
        //just here to check if the total pool size is not larger than Char.MaxValue if (widePacket == false)
        //or if the total size is not larger than Int.MaxValue (if widePacket == true)
        //else throw PacketPoolTooLongException.
        var totalSize: Long = 0

        while (i < len) {
            val chunk = chunks(i)
            val size  = chunk.size
            if (size > 0) {
                putRef(pos + i * refSize, size)
                totalSize += size
                //println(s"Write chunk. pos of ${chunk.tag} = ${buff.position()}")
                writeChunk(i, chunk)
                //println(s"End Write chunk. end pos of ${chunk.tag} = ${buff.position()}")
            }
            i = (i + 1).toByte
        }
        if ((totalSize > scala.Char.MaxValue && !widePacket) || totalSize > scala.Int.MaxValue)
            throw new PacketPoolTooLongException(s"Packet total items size exceeded available size (total size: $totalSize, widePacket: $widePacket)")
    }

    private def writeChunk(flag: Byte, poolChunk: PoolChunk[Any]): Unit = {
        val size = poolChunk.size
        //Write content
        if (flag >= Int && flag < Char) {
            ArrayPersistence.writePrimitiveArrayContent(this, poolChunk.array, flag, 0, size)
            return
        }

        @inline
        def foreach[T](@inline action: T => Unit): Unit = {
            val items = poolChunk.array.asInstanceOf[Array[T]]
            var i     = 0
            while (i < size) {
                val item = items(i)
                //println(s"Writing item $item (pos: ${buff.position()})")
                action(item)
                //println(s"Item Written! (pos: ${buff.position()})")
                i += 1
            }
        }

        (flag: @switch) match {
            case Class | SyncClass => foreach[Class[_]](cl => buff.putInt(ClassMappings.codeOfClass(cl)))
            case String            => foreach[String](putString)
            case Enum              => foreach[Enum[_]](putEnum)
            case Array             => foreach[AnyRef](xs => ArrayPersistence.writeArray(this, xs))
            case ContextRef        => foreach[SimpleContextObject](obj => buff.putInt(obj.refId))
            case Object            => foreach[PacketObject](writeObject)
        }
    }

    def getPool: SerializerPacketObjectPool = pool

    private def putEnum(enum: Enum[_]): Unit = {
        putTypeRef(`enum`.getClass)
        putString(enum.name())
    }

    private def putString(str: String): Unit = {
        buff.putInt(str.length).put(str.getBytes())
    }

    private def writeObject(poolObj: PacketObject): Unit = {
        val value = poolObj.value
        putTypeRef(value)
        config.informObjectSent(value)
        ArrayPersistence.writeArrayContent(this, poolObj.decomposed.asInstanceOf[Array[AnyRef]])
    }

    @inline
    private def putRef(pos: Int, ref: Int): Unit = {
        if (widePacket) buff.putInt(pos, ref)
        else buff.putChar(pos, ref.toChar)
    }

    @inline
    def putRef(ref: Int): Unit = {
        if (widePacket) buff.putInt(ref)
        else buff.putChar(ref.toChar)
    }

    @inline
    def putPoolRef(obj: AnyRef): Unit = {
        val idx = pool.globalPosition(obj)
        putRef(idx)
    }

    def putTypeRef(obj: AnyRef): Unit = {
        obj match {
            case sync: SynchronizedObject[_] => putGeneratedTypeRef(sync.getSuperClass)
            case _                           => putTypeRef(obj.getClass)
        }
    }

    def putTypeRef(tpe: Class[_]): Unit = {
        putRef(pool.getChunkFromFlag(Class).indexOf(tpe))
    }

    private def putGeneratedTypeRef(clazz: Class[_]): Unit = {
        val size = pool.getChunkFromFlag(Class).size
        val idx  = pool.getChunkFromFlag(SyncClass).indexOf(clazz) + size
        putRef(idx)
    }

}

object PacketWriter {
    private val Sizes2B = new Array[Int](ChunkCount).mapInPlace(_ => scala.Char.MaxValue)
    private val Sizes4B = new Array[Int](ChunkCount).mapInPlace(_ => scala.Int.MaxValue)
}
