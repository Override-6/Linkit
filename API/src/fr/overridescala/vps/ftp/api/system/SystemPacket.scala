package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.`extension`.packet.{PacketFactory, PacketUtils}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

class SystemPacket private(override val channelID: Int,
                           override val senderID: String,
                           override val targetID: String,
                           val order: SystemOrder,
                           val reason: Reason) extends Packet

object SystemPacket {

    def apply(order: SystemOrder, reason: Reason)(implicit systemChannel: PacketChannel) =
        new SystemPacket(systemChannel.channelID, systemChannel.ownerID, systemChannel.connectedID, order, reason)

    object Factory extends PacketFactory[SystemPacket] {

        private val TYPE = "[sys]".getBytes
        private val REASON = "<reason>".getBytes

        override def decompose(implicit packet: SystemPacket): Array[Byte] = {
            TYPE ++ packet.order.name().getBytes() ++
                    REASON ++ packet.reason.name().getBytes()
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

        override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): SystemPacket = {
            val orderName = PacketUtils.cutString(TYPE, REASON)
            val reasonName = PacketUtils.cutEndString(REASON)
            new SystemPacket(channelID, senderID, targetID, SystemOrder.valueOf(orderName), Reason.valueOf(reasonName))
        }
    }

}