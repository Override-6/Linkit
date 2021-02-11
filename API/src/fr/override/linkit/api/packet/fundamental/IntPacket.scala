package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class IntPacket(value: Int) extends Packet

object IntPacket extends PacketCompanion[IntPacket]{
    override val identifier: Int = 8
}
