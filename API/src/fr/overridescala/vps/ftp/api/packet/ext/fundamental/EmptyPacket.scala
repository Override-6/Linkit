package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class EmptyPacket private(override val channelID: Int,
                               override val senderID: String,
                               override val targetID: String) extends Packet {

}

object EmptyPacket {
    private val TYPE = "[empty]".getBytes
    private val SENDER = "<sender>".getBytes
    private val TARGET = "<target>".getBytes

    def apply()(implicit channel: PacketChannel): EmptyPacket =
        new EmptyPacket(channel.channelID, channel.ownerIdentifier, channel.connectedIdentifier)

    object Factory extends PacketFactory[EmptyPacket] {
        override def decompose(implicit packet: EmptyPacket): Array[Byte] = {
            val idBytes = s"${packet.channelID}".getBytes
            val senderBytes = packet.senderID.getBytes
            val targetBytes = packet.targetID.getBytes
            TYPE ++ idBytes ++
                    SENDER ++ senderBytes ++
                    TARGET ++ targetBytes
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(TYPE)

        override def build(implicit bytes: Array[Byte]): EmptyPacket = {
            val id = cutString(TYPE, SENDER).toInt
            val sender = cutString(SENDER, TARGET)
            val target = new String(cutEnd(TARGET))
            new EmptyPacket(id, sender, target)
        }

    }

}

