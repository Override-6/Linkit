package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class WrappedPacket(tag: String, subPacket: Packet) extends Packet

object WrappedPacket extends PacketCompanion[WrappedPacket] {
    override val packetClass: Class[WrappedPacket] = classOf[WrappedPacket]
    override val identifier: Int = 5

}
