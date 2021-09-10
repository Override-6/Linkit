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

package fr.linkit.engine.connection.packet.persistence.serializor.read

import java.nio.ByteBuffer

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.pool.SimpleContextObject
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.connection.packet.persistence.serializor.{ArrayPersistence, ClassNotMappedException}
import fr.linkit.engine.local.mapping.ClassMappings

import scala.annotation.switch
import scala.reflect.ClassTag

class PacketReader(config: PacketConfig, context: PersistenceContext, val buff: ByteBuffer) {

    private val (widePacket: Boolean, sizes, pool) = preReadPool()
    private var isInit                             = false

    def initPool(): Unit = {
        if (isInit)
            throw new IllegalStateException("This pool is already initialized.")
        isInit = true
        var i: Byte = 0
        while (i < ChunkCount) {
            val size = sizes(i)
            if (size > 0)
                readNextChunk(size, i)
            i = (i + 1).toByte
        }
    }

    def getPool: DeserializerPacketObjectPool = pool

    @inline
    def readNextGlobalRef: Int = {
        if (widePacket) buff.getInt() else buff.getChar()
    }

    private def readNextChunk(size: Int, flag: Byte): Unit = {
        if (flag >= Int && flag <= Char) {
            val chunk   = pool.getChunkFromFlag[Any](flag)
            val content = ArrayPersistence.readPrimitiveArray(size, flag, this)
            System.arraycopy(content, 0, chunk.array, 0, content.length)
            return
        }

        @inline
        def collectAndUpdateChunk[T: ClassTag](@inline action: => T): Unit = {
            var i     = 0
            val chunk = pool.getChunkFromFlag[Any](flag)
            val array = chunk.array
            while (i < size) {
                array(i) = action
                i += 1
            }
        }

        (flag: @switch) match {
            case Class      => collectAndUpdateChunk[Class[_]](readClass())
            case String     => collectAndUpdateChunk[String](readString())
            case Array      => collectAndUpdateChunk[Array[_]](ArrayPersistence.readArray(this))
            case Object     => collectAndUpdateChunk[NotInstantiatedObject[_]](readObject())
            case ContextRef => collectAndUpdateChunk[SimpleContextObject](readContextObject())
        }
    }

    private def readContextObject(): SimpleContextObject = {
        val id  = buff.getInt()
        val obj = config.getReferenced(id).getOrElse {
            throw new NoSuchElementException(s"Could not find contextual object of identifier '$id' in provided configuration.")
        }
        new SimpleContextObject(id, obj)
    }

    private def readClass(): Class[_] = {
        val code  = buff.getInt
        val clazz = ClassMappings.getClass(code)
        if (clazz == null)
            throw new ClassNotMappedException(s"No class is bound to code $code")
        clazz
    }

    private def preReadPool(): (Boolean, Array[Int], DeserializerPacketObjectPool) = {
        val widePacket = buff.get() == 1
        val sizes      = new Array[Int](ChunkCount)
        var i          = 0
        while (i < ChunkCount) {
            sizes(i) = if (widePacket) buff.getInt() else buff.getChar()
            i += 1
        }
        (widePacket, sizes, new DeserializerPacketObjectPool(sizes))
    }

    private def readObject(): NotInstantiatedObject[AnyRef] = {
        val classCode = buff.getInt()
        val clazz     = ClassMappings.getClass(classCode)
        if (clazz == null)
            throw new ClassNotMappedException(s"Class of code '$classCode' is not mapped.")
        val profile     = config.getProfile[AnyRef](clazz, context)
        // The next int is the content size,
        // we skip the array content for now
        // because we need only parse the object type
        // as we return a NotInstantiatedObject, However, the pos in front
        // of the array content is kept in order to read the object content after
        val contentSize = buff.getInt
        val content     = readObjectContent(contentSize)
        new NotInstantiatedObject[AnyRef](profile, content, pool, clazz)
    }

    private def readObjectContent(length: Int): Array[Int] = {
        var i       = 0
        val content = new Array[Int](length)
        while (i < length) {
            content(i) = readNextGlobalRef
            i += 1
        }
        content
    }

    private def readString(): String = {
        val size  = buff.getInt()
        val array = new Array[Byte](size)
        buff.get(array)
        new String(array)
    }
}
