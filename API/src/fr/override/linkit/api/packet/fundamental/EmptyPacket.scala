package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    override def toString: String = "EmptyPacket"

    object Companion extends PacketCompanion[EmptyPacket] {
        override val packetClass: Class[EmptyPacket] = EmptyPacket.getClass.asInstanceOf[Class[EmptyPacket]]
        override val identifier: Int = 2
    }

}
