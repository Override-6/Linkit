/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.packet.PacketAttributes
import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.engine.gnom.persistence.context.Persist

import java.io.Serializable
import scala.collection.mutable

class SimplePacketAttributes extends PacketAttributes with Deconstructible {

    protected[packet] val attributes: mutable.Map[Serializable, Serializable] = mutable.HashMap[Serializable, Serializable]()

    def this(attributes: SimplePacketAttributes) = {
        this()
        attributes.attributes.foreachEntry(putAttribute)
    }

    @Persist
    def this(array: Array[(Serializable, Serializable)]) = {
        this()
        array.foreach(x => putAttribute(x._1, x._2))
    }

    override def getAttribute[S](name: Serializable): Option[S] = attributes.get(name) match {
        case o: Option[S] => o
        case _            => None
    }

    override def putAttribute(name: Serializable, value: Serializable): this.type = {
        attributes.put(name, value)
        //AppLogger.vError(s"Attribute put ($name -> $value), $attributes - $hashCode")
        this
    }

    override def equals(obj: Any): Boolean = obj match {
        case s: SimplePacketAttributes => s.attributes == attributes
        case _                         => false
    }

    override def hashCode(): Int = attributes.hashCode()

    override def toString: String = attributes.mkString("SimplePacketAttributes(", ", ", ")")

    override def drainAttributes(other: PacketAttributes): this.type = {
        foreachAttributes((k, v) => other.putAttribute(k, v))
    }

    override def foreachAttributes(f: (Serializable, Serializable) => Unit): this.type = {
        attributes.foreachEntry(f)
        this
    }

    override def isEmpty: Boolean = attributes.isEmpty

    override def deconstruct(): Array[Any] = attributes.toArray
}

object SimplePacketAttributes {

    def empty: SimplePacketAttributes = new SimplePacketAttributes

    def apply(): SimplePacketAttributes = empty

    def apply(attributes: SimplePacketAttributes): SimplePacketAttributes = new SimplePacketAttributes(attributes)

    def apply(tuples: (String, Serializable)*): SimplePacketAttributes = {
        val atr = empty
        tuples.foreach(tuple => atr.putAttribute(tuple._1, tuple._2))
        atr
    }
}
