package fr.`override`.linkit.core.connection.packet.fundamental

import fr.`override`.linkit.api.connection.packet.Packet

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
