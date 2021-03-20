/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

sealed trait RefPacket[A <: Serializable] extends Packet {
    val value: A
    def casted[C <: A]: C = value.asInstanceOf[C]
}

object RefPacket {

    case class StringPacket(override val value: String) extends RefPacket[String]

    case class AnyRefPacket[A <: Serializable] private(override val value: A) extends RefPacket[A]

    case class ObjectPacket(override val value: Serializable) extends RefPacket[Serializable]

    //TODO Fix Array[Serializable] and Array[Any] cast exception
    case class ArrayRefPacket(override val value: Array[Any]) extends RefPacket[Array[Any]] {
        def apply(i: Int): Any = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: Any): Boolean = value.contains(a)

        def length: Int = value.length

        override def toString: String = s"ArrayRefPacket(${value.mkString(",")})"
    }

    case class ArrayValPacket[A <: AnyVal](override val value: Array[A]) extends RefPacket[Array[A]] {
        def apply(i: Int): A = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: A): Boolean = value.contains(a)

        def length: Int = value.length
    }

    def apply[A <: Serializable](value: A): AnyRefPacket[A] = AnyRefPacket(value)

    implicit def unbox[A <: Serializable](packet: RefPacket[A]): A = packet.value

}
