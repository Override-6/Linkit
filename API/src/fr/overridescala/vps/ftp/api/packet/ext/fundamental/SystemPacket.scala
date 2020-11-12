package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

class SystemPacket private(override val channelID: Int,
                           override val senderID: String,
                           override val targetID: String,
                           val order: String) extends Packet

object SystemPacket {

    val ClientClose = "CLIENT_CLOSE"
    val ServerClose = "SERVER_CLOSE"
    val ClientInitialisation = "INIT_TO_SERVER"

    def apply(order: String)(implicit systemChannel: PacketChannel) =
        new SystemPacket(systemChannel.channelID, systemChannel.ownerID, systemChannel.connectedID, order)

    object Factory extends PacketFactory[SystemPacket] {

        private val TYPE = "[sys]".getBytes

        override def decompose(implicit packet: SystemPacket): Array[Byte] = {
            TYPE ++ packet.order.getBytes
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

        override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): SystemPacket = {
            val orderBytes = bytes.slice(TYPE.length, bytes.length)
            new SystemPacket(channelID, senderID, targetID, new String(orderBytes))
        }
    }

}
