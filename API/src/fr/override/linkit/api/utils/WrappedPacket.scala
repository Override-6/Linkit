package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator, PacketUtils}

case class WrappedPacket(category: String, subPacket: Packet) extends Packet
object WrappedPacket extends PacketFactory[WrappedPacket] {

    override val packetClass: Class[WrappedPacket] = classOf[WrappedPacket]
    private val Type = "[fpckt]".getBytes
    private val FragPacket = "<frag>".getBytes

    override def decompose(translator: PacketTranslator)(implicit packet: WrappedPacket): Array[Byte] = {
        val subPacketBytes = translator.fromPacket(packet.subPacket)
        val headerBytes = packet.category.getBytes
        Type ++ headerBytes ++ FragPacket ++ subPacketBytes
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(Type)

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): WrappedPacket = {
        val header = PacketUtils.stringBetween(Type, FragPacket)
        val subPacket = translator.toPacket(PacketUtils.untilEnd(FragPacket))
        new WrappedPacket(header, subPacket)
    }
}
