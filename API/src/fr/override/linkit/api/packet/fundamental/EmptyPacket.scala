package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator}

object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    object Factory extends PacketFactory[EmptyPacket] {
        override def decompose(translator: PacketTranslator)(implicit packet: EmptyPacket): Array[Byte] =
            new Array[Byte](0)

        override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = bytes.isEmpty

        override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): EmptyPacket = EmptyPacket

        override val packetClass: Class[EmptyPacket] = EmptyPacket.getClass.asInstanceOf[Class[EmptyPacket]]
    }

    override def toString: String = "EmptyPacket"

}

