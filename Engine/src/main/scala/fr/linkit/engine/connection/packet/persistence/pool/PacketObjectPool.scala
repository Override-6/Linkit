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

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext, TypeProfile}
import fr.linkit.engine.connection.packet.persistence.pool.PacketObjectPool.{ContextObject, PacketObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._

class PacketObjectPool(config: PacketConfig, context: PersistenceContext, sizes: Array[Char]) {

    protected val chunks: Array[PoolChunk[_]] = scala.Array[PoolChunk[_]](
        // Objects types
        new PoolChunk[Class[_]](sizes(Class)),
        // Context Objects identifiers
        new PoolChunk[Int](sizes(Int)),
        // Strings
        new PoolChunk[String](sizes(String)),
        // Primitives (excepted primitives stored into primitives arrays)
        new PoolChunk[Int](sizes(Int)),
        new PoolChunk[Short](sizes(Short)),
        new PoolChunk[Long](sizes(Long)),
        new PoolChunk[Byte](sizes(Byte)),
        new PoolChunk[Double](sizes(Double)),
        new PoolChunk[Float](sizes(Float)),
        new PoolChunk[Boolean](sizes(Boolean)),
        new PoolChunk[Char](sizes(Char)),
        // Arrays
        new PoolChunk[Array[_]](sizes(Array)),
        // Objects
        new PoolChunk[PacketObject](sizes(Object))
    )

    def indexOf(ref: AnyRef): Int = {
        getChunk(ref).indexOf(ref)
    }

    @inline
    private def getChunk[T](idx: Byte): PoolChunk[T] = {
        chunks(idx).asInstanceOf[PoolChunk[T]]
    }

    def getContextRefChunk: PoolChunk[ContextObject] = getChunk(ContextRef)

    def getChunk[T](ref: Any): PoolChunk[T] = {
        ref match {
            case _: Int     => getChunk(Int)
            case _: Byte    => getChunk(Int)
            case _: Short   => getChunk(Int)
            case _: Long    => getChunk(Long)
            case _: Double  => getChunk(Double)
            case _: Float   => getChunk(Float)
            case _: Boolean => getChunk(Boolean)
            case _: Char    => getChunk(Char)

            case _: Class[_]   => getChunk(Class)
            case _: String     => getChunk(String)
            case _: Array[Any] => getChunk(Array)
            case _             => getChunk(Object)
        }
    }

    def indexOfClass(ref: Class[_]): Int = {
        val chunk = getChunk[Class[_]](Class)
        val idx   = chunk.indexOf(ref)
        if (idx == -1) {
            chunk.add(ref)
            chunk.size
        } else idx
    }

    def addObject(ref: AnyRef): Unit = {
        if (indexOf(ref) >= 0)
            return
        ref match {
            case _: String     =>
                getChunk(String).add(ref)
            case a: Array[Any] =>
                getChunk(Array).add(a)
            case _             =>
                val profile = config.getProfile[AnyRef](ref.getClass, context)
                val code    = config.getReferencedCode(ref)
                if (code.isEmpty) {
                    val decomposed = profile.toArray(ref)
                    val objPool    = getChunk[PacketObject](Object)
                    objPool.add(PacketObject(ref, objPool.size, getTypeRef(ref.getClass), decomposed, profile))
                    addAll(decomposed)
                } else {
                    val pool = getChunk[ContextObject](ContextRef)
                    pool.add(ContextObject(ref, pool.size, code.get))
                }
        }
    }

    private def getTypeRef(tpe: Class[_]): Char = {
        val tpePool = getChunk[Class[_]](Class)
        var idx     = tpePool.indexOf(tpe).toChar
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
            objects(i) match {
                case subRef: AnyRef => addObject(subRef)
            }
            i += 1
        }
    }

    def getChunks: Array[PoolChunk[Any]] = chunks.asInstanceOf[Array[PoolChunk[Any]]]

}

object PacketObjectPool {
    // The number of different types that can be serialized
    // (based on the number of constants in ConstantProtocol, excepted Null flag)
    val ChunkCount = 14

    trait PoolObject[T] {

        def value: T

        val poolIndex: Char
    }

    case class ContextObject(override val value: Any,
                             override val poolIndex: Char,
                             refInt: Int) extends PoolObject[Any]

    case class PacketObject(override val value: Any,
                            override val poolIndex: Char,
                            typePoolIndex: Char,
                            decomposed: Array[Any], profile: TypeProfile[_]) extends PoolObject[Any] {

        override def equals(obj: Any): Boolean = obj == this.value

        override def hashCode(): Int = value.hashCode()
    }

}

