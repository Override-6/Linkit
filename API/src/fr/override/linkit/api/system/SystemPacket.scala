package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator, PacketUtils}

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet

object SystemPacket extends PacketFactory[SystemPacket] {

    private val TYPE = "[sys]".getBytes
    private val REASON = "<reason>".getBytes
    private val CONTENT = "<content>".getBytes

    override def decompose(translator: PacketTranslator)(implicit packet: SystemPacket): Array[Byte] = {
        TYPE ++ packet.order.name().getBytes() ++
                REASON ++ packet.reason.name().getBytes() ++
                CONTENT ++ packet.content
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): SystemPacket = {
        val orderName = PacketUtils.stringBetween(TYPE, REASON)
        val reasonName = PacketUtils.stringBetween(REASON, CONTENT)
        val content = PacketUtils.untilEnd(CONTENT)

        val systemOrder = SystemOrder.valueOf(orderName)
        val reason = CloseReason.valueOf(reasonName)
        new SystemPacket(systemOrder, reason, content)
    }

    override val packetClass: Class[SystemPacket] = classOf[SystemPacket]
}