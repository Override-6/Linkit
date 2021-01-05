package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketUtils}

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet

object SystemPacket extends PacketFactory[SystemPacket] {

    private val TYPE = "[sys]".getBytes
    private val REASON = "<reason>".getBytes
    private val CONTENT = "<content>".getBytes

    override def decompose(implicit packet: SystemPacket): Array[Byte] = {
        TYPE ++ packet.order.name().getBytes() ++
                REASON ++ packet.reason.name().getBytes() ++
                CONTENT ++ packet.content
    }

    override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

    override def build(implicit bytes: Array[Byte]): SystemPacket = {
        val orderName = PacketUtils.cutString(TYPE, REASON)
        val reasonName = PacketUtils.cutString(REASON, CONTENT)
        val content = PacketUtils.cutEnd(CONTENT)

        val systemOrder = SystemOrder.valueOf(orderName)
        val reason = CloseReason.valueOf(reasonName)
        new SystemPacket(systemOrder, reason, content)
    }

    override val packetClass: Class[SystemPacket] = classOf[SystemPacket]
}