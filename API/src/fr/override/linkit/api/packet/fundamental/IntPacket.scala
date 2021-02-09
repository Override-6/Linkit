package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketSerialisationStrategy}
import fr.`override`.linkit.api.utils.ScalaUtils

case class IntPacket(value: Int) extends Packet

object IntPacket extends PacketCompanion[IntPacket] with PacketSerialisationStrategy[IntPacket] {
    override val identifier: Int = 8

    override def serialize(packet: IntPacket): Array[Byte] = ScalaUtils.fromInt(packet.value)

    override def deserialize(bytes: Array[Byte]): IntPacket = IntPacket(ScalaUtils.toInt(bytes))
}
