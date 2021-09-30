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

package fr.linkit.engine.gnom.persistence.pool

import fr.linkit.api.gnom.persistence.Freezable
import fr.linkit.api.gnom.persistence.obj.{ReferencedNetworkObject, InstanceObject}
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._

class ObjectPool(sizes: Array[Int]) extends Freezable {

    private var frozen = false

    protected val chunks: Array[PoolChunk[_]] = scala.Array[PoolChunk[_]](
        // Objects types
        new PoolChunk[Class[_]](Class, this, sizes(Class)),
        new PoolChunk[Class[_]](SyncClass, this, sizes(SyncClass)),
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
        // Objects
        new PoolChunk[Enum[_]](Enum, this, sizes(Enum)),
        new PoolChunk[InstanceObject[AnyRef]](Object, this, sizes(Object)),
        // Context Objects Locations
        new PoolChunk[ReferencedNetworkObject](RNO, this, sizes(RNO)),
        // Arrays
        new PoolChunk[Array[_]](Array, this, sizes(Array))
    )

    override def freeze(): Unit = frozen = true

    override def isFrozen: Boolean = frozen

    @inline
    def getChunkFromFlag[T](idx: Byte): PoolChunk[T] = {
        chunks(idx).asInstanceOf[PoolChunk[T]]
    }

    def getContextRefChunk: PoolChunk[ReferencedNetworkObject] = getChunkFromFlag(RNO)

    def getChunks: Array[PoolChunk[Any]] = chunks.asInstanceOf[Array[PoolChunk[Any]]]

}

object ObjectPool {

}

