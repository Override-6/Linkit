package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketSerialisationStrategy}

object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    override def toString: String = "EmptyPacket"

    object Companion extends PacketCompanion[EmptyPacket] with PacketSerialisationStrategy[EmptyPacket] {
        override val identifier: Int = 2

        override def serialize(packet: EmptyPacket): Array[Byte] = Array()

        override def deserialize(bytes: Array[Byte]): EmptyPacket = EmptyPacket
    }

}
