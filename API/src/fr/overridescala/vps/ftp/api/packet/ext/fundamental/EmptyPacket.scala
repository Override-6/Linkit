package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class EmptyPacket private(override val channelID: Int,
                               override val senderID: String,
                               override val targetID: String) extends Packet {

}

object EmptyPacket {
    private val TYPE = "[empty]".getBytes

    def apply()(implicit channel: PacketChannel): EmptyPacket =
        new EmptyPacket(channel.channelID, channel.ownerIdentifier, channel.connectedIdentifier)

    object Factory extends PacketFactory[EmptyPacket] {

        import fr.overridescala.vps.ftp.api.packet.ext.PacketUtils._

        override def decompose(implicit packet: EmptyPacket): Array[Byte] = {
            val channelID = s"${packet.channelID}".getBytes
            TYPE ++ channelID
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.containsSlice(TYPE)

        override def build(senderID: String, targetId: String)(implicit bytes: Array[Byte]): EmptyPacket = {
            val channelID = new String(cutEnd(TYPE)).toInt
            new EmptyPacket(channelID, senderID, targetId)
        }

    }

}

