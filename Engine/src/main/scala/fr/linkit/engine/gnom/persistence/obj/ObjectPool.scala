/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.persistence.Freezable
import fr.linkit.api.gnom.persistence.obj.{LambdaObject, SyncPoolObject, ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.engine.gnom.persistence.ConstantProtocol._

import java.lang

abstract class ObjectPool(sizes: Array[Int]) extends Freezable {
    
    private var frozen = false
    
    protected val chunks: Array[PoolChunk[_]] = scala.Array[PoolChunk[_]](
        // Objects classes
        new PoolChunk[Class[_]](Class, true, this, sizes(Class)),
        new PoolChunk[SyncClassDef](SyncDef, true, this, sizes(SyncDef)),
        // Strings
        new PoolChunk[String](String, true, this, sizes(String)),
        // Primitives (primitives stored into primitives arrays are not contained in those chunks)
        new PoolChunk[Int](Int, true, this, sizes(Int)),
        new PoolChunk[Short](Short, true, this, sizes(Short)),
        new PoolChunk[Long](Long, true, this, sizes(Long)),
        new PoolChunk[Byte](Byte, true, this, sizes(Byte)),
        new PoolChunk[Double](Double, true, this, sizes(Double)),
        new PoolChunk[Float](Float, true, this, sizes(Float)),
        new PoolChunk[Boolean](Boolean, true, this, sizes(Boolean)),
        new PoolChunk[Char](Char, true, this, sizes(Char)),
        // Objects
        new PoolChunk[Enum[_]](Enum, true, this, sizes(Enum)),
        new PoolChunk[ProfilePoolObject[AnyRef]](Object, false, this, sizes(Object)),
        // Arrays
        new ArrayPoolChunk(Array, this, sizes(Array)),
        // Context Objects Locations
        new PoolChunk[ReferencedPoolObject](RNO, false, this, sizes(RNO))
        )
    
    override def freeze(): Unit = frozen = true
    
    override def isFrozen: Boolean = frozen
    
    @inline
    def getChunkFromFlag[T](idx: Byte): PoolChunk[T] = {
        chunks(idx).asInstanceOf[PoolChunk[T]]
    }
    
    def getContextRefChunk: PoolChunk[ReferencedPoolObject] = getChunkFromFlag(RNO)
    
    def getChunks: Array[PoolChunk[Any]] = chunks.asInstanceOf[Array[PoolChunk[Any]]]
    
}