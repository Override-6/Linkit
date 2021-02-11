package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class StringPacket(value: String) extends Packet

object StringPacket extends PacketCompanion[StringPacket] {
    override val identifier: Int = 7

}
