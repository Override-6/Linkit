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

package fr.linkit.engine.connection.packet.persistence.pool

import fr.linkit.api.connection.packet.persistence.Freezable
import fr.linkit.api.connection.packet.persistence.obj.{ContextObject, InstanceObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._

class PacketObjectPool(sizes: Array[Int]) extends Freezable {

    private var frozen = false

    protected val chunks: Array[PoolChunk[_]] = scala.Array[PoolChunk[_]](
        // Objects types
        new PoolChunk[Class[_]](Class, this, sizes(Class)),
        // Context Objects identifiers
        new PoolChunk[ContextObject](ContextRef, this, sizes(ContextRef)),
        // Strings
        new PoolChunk[String](String, this, sizes(String)),
        // Primitives (excepted primitives stored into primitives arrays)
        new PoolChunk[Int](Int, this, sizes(Int)),
        new PoolChunk[Short](Short, this, sizes(Short)),
        new PoolChunk[Long](Long, this, sizes(Long)),
        new PoolChunk[Byte](Byte, this, sizes(Byte)),
        new PoolChunk[Double](Double, this, sizes(Double)),
        new PoolChunk[Float](Float, this, sizes(Float)),
        new PoolChunk[Boolean](Boolean, this, sizes(Boolean)),
        new PoolChunk[Char](Char, this, sizes(Char)),
        // Arrays
        new PoolChunk[Array[_]](Array, this, sizes(Array)),
        // Objects
        new PoolChunk[InstanceObject[AnyRef]](Object, this, sizes(Object))
    )

    override def freeze(): Unit = {
        frozen = true
    }

    override def isFrozen: Boolean = frozen

    @inline
    def getChunkFromFlag[T](idx: Byte): PoolChunk[T] = {
        chunks(idx).asInstanceOf[PoolChunk[T]]
    }

    def getContextRefChunk: PoolChunk[ContextObject] = getChunkFromFlag(ContextRef)

    def getChunks: Array[PoolChunk[Any]] = chunks.asInstanceOf[Array[PoolChunk[Any]]]

}

object PacketObjectPool {

}

