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

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext, TypeProfile}
import fr.linkit.engine.connection.packet.persistence.serializor.write.PacketObjectPool._
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._

class PacketObjectPool(config: PacketConfig, context: PersistenceContext) {

    private var totalSize: Int = 0
    private val chunks        = new Array[PoolChunk[_]](ChunkCount)

    def indexOf(ref: AnyRef): Int = {
        var i = 0
        while (i <= pos && i <= Char.MaxValue) {
            val registered = chunks(i)
            if (registered != null && (registered.value == ref))
                return i
            i += 1
        }
        -1
    }

    @inline
    private def getChunk[T](idx: Int): PoolChunk[T] = {
        chunks(idx).asInstanceOf[PoolChunk[T]]
    }

    def indexOfClass(ref: Class[_]): Int = {
        val chunk = getChunk[Class[_]](Class)
        val idx = chunk.indexOf(ref)
        if (idx == -1) {
            chunk.add(ref)
            chunk.size
        } else idx
    }

    def add(ref: AnyRef): Unit = {
        if (indexOf(ref) >= 0)
            return
        ref match {
            case _: String     =>
                addOne(ref, Array(ref), null)
            case a: Array[Any] =>
                addOne(ref, a, null)
            case _             =>
                val profile    = config.getProfile[AnyRef](ref.getClass, context)
                val decomposed = profile.toArray(ref)
                addOne(ref, decomposed, profile)
                addAll(decomposed)
        }
    }

    @inline private def addClass(clazz: Class[_]): Unit = {
        chunks(pos) = ObjectType(clazz, pos.toChar)
        pos += 1
    }

    private def addOne(ref: AnyRef, decomposed: Array[Any], profile: TypeProfile[_]): Unit = {
        val code = config.getReferencedCode(ref)
        val p    = pos.toChar
        if (code.isEmpty) {
            addClass(ref.getClass)
            chunks(pos) = PacketObject(ref, p, decomposed, profile)
        } else {
            chunks(pos) = ContextObject(ref, p, code.get)
        }
        pos += 1
    }

    private def addAll(objects: Array[Any]): Unit = {
        var i   = 0
        val len = objects.length
        while (i < len) {
            objects(i) match {
                case subRef: AnyRef => add(subRef)
            }
            i += 1
        }
    }

    def getType(i: Int): Class[_] = {
        apply(i) match {
            case clazz: ObjectType => clazz.value
            case _                 => throw new ClassCastException(s"Could not cast object at index ${i} to a Java Class")
        }
    }

    def apply(i: Int): PoolObject[_] = {
        if (i >= pos)
            throw new IndexOutOfBoundsException(s"$i >= $pos")
        chunks(i)
    }

    def size: Int = pos

}

object PacketObjectPool {
    // The number of different types that can be serialized
    // (based on the number of constants in ConstantProtocol, excepted Null flag)
    val ChunkCount = 14
    trait PoolObject[T] {

        def value: T

        val poolIndex: Char
    }

    case class ObjectType(override val value: Class[_],
                          override val poolIndex: Char) extends PoolObject[Class[_]]

    case class ContextObject(override val value: Any,
                             override val poolIndex: Char,
                             refInt: Int) extends PoolObject[Any]

    case class PacketObject(override val value: Any,
                            override val poolIndex: Char,
                            decomposed: Array[Any], profile: TypeProfile[_]) extends PoolObject[Any] {

        override def equals(obj: Any): Boolean = obj == this.value

        override def hashCode(): Int = value.hashCode()
    }

}

