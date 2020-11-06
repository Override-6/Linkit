package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class EmptyPacket private(override val channelID: Int,
                               override val senderID: String,
                               override val targetID: String) extends Packet {

}

object EmptyPacket {

    def apply()(implicit channel: PacketChannel): EmptyPacket =
        new EmptyPacket(channel.channelID, channel.ownerID, channel.connectedID)

    object Factory extends PacketFactory[EmptyPacket] {

        override def decompose(implicit packet: EmptyPacket): Array[Byte] =
            new Array[Byte](0)

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.isEmpty

        override def build(channelID: Int, senderID: String, targetId: String)(implicit bytes: Array[Byte]): EmptyPacket =
            new EmptyPacket(channelID, senderID, targetId)

    }

}

