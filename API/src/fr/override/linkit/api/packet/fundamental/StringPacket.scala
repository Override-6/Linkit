package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketSerialisationStrategy}

case class StringPacket(value: String) extends Packet

object StringPacket extends PacketCompanion[StringPacket] with PacketSerialisationStrategy[StringPacket] {
    override val identifier: Int = 7

    override def serialize(packet: StringPacket): Array[Byte] = packet.value.getBytes

    override def deserialize(bytes: Array[Byte]): StringPacket = StringPacket(new String(bytes))

}
