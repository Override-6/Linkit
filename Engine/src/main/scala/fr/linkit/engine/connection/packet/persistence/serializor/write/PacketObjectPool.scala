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

class PacketObjectPool(config: PacketConfig, context: PersistenceContext) {

    private var pos: Int = 0
    private val objects  = new Array[PoolObject](Char.MaxValue)

    def indexOf(ref: AnyRef): Int = {
        var i = 0
        while (i <= pos && i <= Char.MaxValue) {
            val registered = objects(i)
            if (registered != null && (registered.obj == ref))
                return i
            i += 1
        }
        -1
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

    private def addOne(ref: AnyRef, decomposed: Array[Any], profile: TypeProfile[_]): Unit = {
        val code = config.getReferencedCode(ref)
        val p    = pos.toChar
        if (code.isEmpty) {
            objects(pos) = PacketObject(ref, p, decomposed, profile)
        } else {
            objects(pos) = ContextObject(ref, p, code.get)
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

    def apply(i: Int): PoolObject = {
        if (i >= pos)
            throw new IndexOutOfBoundsException(s"$i >= $pos")
        objects(i)
    }

    def size: Int = pos

}

object PacketObjectPool {

    trait PoolObject {

        def obj: Any

        val poolIndex: Char
    }

    case class ContextObject(override val obj: Any,
                             override val poolIndex: Char,
                             refInt: Int) extends PoolObject

    case class PacketObject(override val obj: Any,
                            override val poolIndex: Char,
                            decomposed: Array[Any], profile: TypeProfile[_]) extends PoolObject {

        override def equals(obj: Any): Boolean = obj == this.obj

        override def hashCode(): Int = obj.hashCode()
    }
}

