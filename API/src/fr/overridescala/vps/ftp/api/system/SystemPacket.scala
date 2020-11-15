package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

class SystemPacket private(override val channelID: Int,
                           override val senderID: String,
                           override val targetID: String,
                           val order: SystemOrder) extends Packet

object SystemPacket {

    def apply(order: SystemOrder)(implicit systemChannel: PacketChannel) =
        new SystemPacket(systemChannel.channelID, systemChannel.ownerID, systemChannel.connectedID, order)

    object Factory extends PacketFactory[SystemPacket] {

        private val TYPE = "[sys]".getBytes

        override def decompose(implicit packet: SystemPacket): Array[Byte] = {
            TYPE ++ packet.order.name().getBytes()
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

        override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): SystemPacket = {
            val orderBytes = bytes.slice(TYPE.length, bytes.length)
            new SystemPacket(channelID, senderID, targetID, SystemOrder.valueOf(new String(orderBytes)))
        }
    }

}