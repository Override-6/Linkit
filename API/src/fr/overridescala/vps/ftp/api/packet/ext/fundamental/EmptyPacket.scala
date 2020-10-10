package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory

class EmptyPacket private (override val channelID: Int,
                  override val senderIdentifier: String,
                  override val targetIdentifier: String) extends Packet {

}

object EmptyPacket {
    private val TYPE = "[empty]".getBytes
    private val SENDER = "<sender>".getBytes
    private val TARGET = "<target>".getBytes

    def apply()(implicit channel: PacketChannel): EmptyPacket =
        new EmptyPacket(channel.channelID, channel.ownerIdentifier, channel.connectedIdentifier)

    object Factory extends PacketFactory[EmptyPacket] {
        override def toBytes(implicit packet: EmptyPacket): Array[Byte] = {
            TYPE ++ toBytesUnsigned
        }

        def toBytesUnsigned(implicit packet: Packet): Array[Byte] = {
            val idBytes = s"${packet.channelID}".getBytes
            val senderBytes = packet.senderIdentifier.getBytes
            val targetBytes = packet.targetIdentifier.getBytes
            idBytes ++
                    SENDER ++ senderBytes ++
                    TARGET ++ targetBytes
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): EmptyPacket = {
            val id = new String(bytes.slice(0, bytes.indexOfSlice(SENDER))).toInt
            val sender = cutString(SENDER, TARGET)
            val target = new String(cutEnd(TARGET))
            new EmptyPacket(id, sender, target)
        }
    }

}

