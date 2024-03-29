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

package fr.linkit.engine.gnom.packet.fundamental

import fr.linkit.api.gnom.packet.Packet

sealed trait ValPacket[A <: AnyVal] extends Packet {

    val value: A

    def apply: A = value
}

object ValPacket {

    case class BytePacket(override val value: Byte) extends ValPacket[Byte]

    case class ShortPacket(override val value: Short) extends ValPacket[Short]

    case class IntPacket(override val value: Int) extends ValPacket[Int]

    case class LongPacket(override val value: Long) extends ValPacket[Long]

    case class DoublePacket(override val value: Double) extends ValPacket[Double]

    case class FloatPacket(override val value: Float) extends ValPacket[Float]

    case class BooleanPacket(override val value: Boolean) extends ValPacket[Boolean]

    implicit def unbox[A <: AnyVal](packet: ValPacket[A]): A = packet.value

}
