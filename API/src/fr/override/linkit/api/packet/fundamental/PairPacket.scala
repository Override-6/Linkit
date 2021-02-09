package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class PairPacket(tag: String, value: Any) extends Packet {
    def casted[A]: A = value.asInstanceOf[A]
}

object PairPacket extends PacketCompanion[PairPacket] {
    override val identifier: Int = 1
}
