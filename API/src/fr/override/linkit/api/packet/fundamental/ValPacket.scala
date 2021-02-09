package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class ValPacket(value: Serializable) extends Packet {
    def casted[T <: Serializable]: T = value.asInstanceOf[T]
}

object ValPacket extends PacketCompanion[ValPacket] {
    override val identifier: Int = 6
}
