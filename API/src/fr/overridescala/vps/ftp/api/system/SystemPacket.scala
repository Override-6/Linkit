package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.`extension`.packet.{PacketFactory, PacketUtils}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

class SystemPacket private(override val channelID: Int,
                           override val senderID: String,
                           override val targetID: String,
                           val order: SystemOrder,
                           val reason: Reason,
                           val content: Array[Byte]) extends Packet

object SystemPacket {

    def apply(order: SystemOrder, reason: Reason, content: Array[Byte] = Array())(implicit systemChannel: PacketChannel) =
        new SystemPacket(systemChannel.channelID, systemChannel.ownerID, systemChannel.connectedID, order, reason, content)

    object Factory extends PacketFactory[SystemPacket] {

        private val TYPE = "[sys]".getBytes
        private val REASON = "<reason>".getBytes
        private val CONTENT = "<content>".getBytes

        override def decompose(implicit packet: SystemPacket): Array[Byte] = {
            TYPE ++ packet.order.name().getBytes() ++
                    REASON ++ packet.reason.name().getBytes() ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

        override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): SystemPacket = {
            val orderName = PacketUtils.cutString(TYPE, REASON)
            val reasonName = PacketUtils.cutString(REASON, CONTENT)
            val content = PacketUtils.cutEnd(CONTENT)

            val systemOrder = SystemOrder.valueOf(orderName)
            val reason = Reason.valueOf(reasonName)
            new SystemPacket(channelID, senderID, targetID, systemOrder, reason, content)
        }
    }

}