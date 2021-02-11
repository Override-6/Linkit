package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class WrappedPacket(tag: String, subPacket: Packet) extends Packet

object WrappedPacket extends PacketCompanion[WrappedPacket] {
    override val identifier: Int = 5
}
