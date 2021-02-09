package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet

object SystemPacket extends PacketCompanion[SystemPacket] {
    override val identifier: Int = 4
}