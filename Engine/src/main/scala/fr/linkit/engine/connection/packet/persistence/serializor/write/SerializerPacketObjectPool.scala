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
import fr.linkit.api.connection.packet.persistence.obj.{ContextObject, InstanceObject}
import fr.linkit.engine.connection.packet.persistence.pool.{PacketObjectPool, PoolChunk, SimpleContextObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol.{Array, Boolean, Char, Class, ContextRef, Double, Float, Int, Long, Object, String}

class SerializerPacketObjectPool(config: PacketConfig, context: PersistenceContext, sizes: Array[Int]) extends PacketObjectPool(sizes) {
    def getChunk[T](ref: Any): PoolChunk[T] = {
        ref match {
            case _: Int     => getChunkFromFlag(Int)
            case _: Byte    => getChunkFromFlag(Int)
            case _: Short   => getChunkFromFlag(Int)
            case _: Long    => getChunkFromFlag(Long)
            case _: Double  => getChunkFromFlag(Double)
            case _: Float   => getChunkFromFlag(Float)
            case _: Boolean => getChunkFromFlag(Boolean)
            case _: Char    => getChunkFromFlag(Char)

            case _: Class[_]   => getChunkFromFlag(Class)
            case _: String     => getChunkFromFlag(String)
            case _: Array[Any] => getChunkFromFlag(Array)
            case _             => getChunkFromFlag(Object)
        }
    }

    override def freeze(): Unit = {
        super.freeze()
        var i   = 0
        val len = globalShifts.length
        while (i < len) {
            val chunkSize = chunks(i).size
            if (i == 0)
                globalShifts(i) = chunkSize
            else
                globalShifts(i) = globalShifts(i - 1) + chunkSize
            i += 1
        }
    }

    protected val globalShifts                = new Array[Int](chunks.length)

    /**
     * Returns the global index of the reference object, or -1 of the object is not stored into this pool.
     * @throws IllegalArgumentException if this pool is not frozen.
     * */
    def globalIndexOf(ref: AnyRef): Int = {
        if (!isFrozen)
            throw new IllegalStateException("Could not get global Index of ref: This pool is not frozen !")
        val chunk = getChunk[AnyRef](ref)
        chunk.indexOf(ref) + globalShifts(chunk.tag)
    }

    def addObject(ref: AnyRef): Unit = {
        if (isFrozen)
            throw new IllegalStateException("Could not add object: This pool is frozen !")
        addObj(ref)
    }

    private def addObj(ref: AnyRef): Unit = {
        if (globalIndexOf(ref) >= 0)
            return
        ref match {
            case _: String     =>
                getChunkFromFlag(String).add(ref)
            case a: Array[Any] =>
                getChunkFromFlag(Array).add(a)
            case _             =>
                val profile = config.getProfile[AnyRef](ref.getClass, context)
                val code    = config.getReferencedCode(ref)
                if (code.isEmpty) {
                    val decomposed = profile.toArray(ref)
                    val objPool    = getChunkFromFlag[InstanceObject[AnyRef]](Object)
                    objPool.add(new PacketObject(ref, objPool.size, getTypeRef(ref.getClass), decomposed, profile))
                    addAll(decomposed)
                } else {
                    val pool = getChunkFromFlag[ContextObject](ContextRef)
                    pool.add(new SimpleContextObject(code.get, ref, pool.size))
                }
        }
    }

    private def getTypeRef(tpe: Class[_]): Int = {
        val tpePool = getChunkFromFlag[Class[_]](Class)
        var idx     = tpePool.indexOf(tpe)
        if (idx == -1) {
            idx = tpePool.size
            tpePool.add(tpe)
        }
        idx
    }

    private def addAll(objects: Array[Any]): Unit = {
        var i   = 0
        val len = objects.length
        while (i < len) {
            addObj(objects(i).asInstanceOf[AnyRef])
            i += 1
        }
    }
}
