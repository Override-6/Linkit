package fr.overridescala.linkkit.api.packet.fundamental

import fr.overridescala.linkkit.api.`extension`.packet.PacketFactory
import fr.overridescala.linkkit.api.packet.Packet
import fr.overridescala.linkkit.api.`extension`.packet.PacketFactory

object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    object Factory extends PacketFactory[EmptyPacket] {
        override def decompose(implicit packet: EmptyPacket): Array[Byte] =
            new Array[Byte](0)

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.isEmpty

        override def build(implicit bytes: Array[Byte]): EmptyPacket = EmptyPacket

        override val packetClass: Class[EmptyPacket] = EmptyPacket.getClass.asInstanceOf[Class[EmptyPacket]]
    }

    override def toString: String = "EmptyPacket"

}

