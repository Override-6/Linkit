package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

//TODO Doc
/**
 * this class is used to represent a packet to send or to receive.
 * It allows the user and the program to work easier with the packets.
 * a DataPacket can only be send into tasks
 *
 * @param header  the header of the packet, or the type of this data. Headers allows to classify packets / data to send or receive
 * @param content the content of this packet. can be an [[Object]], a [[String]] or whatever. default content is empty
 * */
case class DataPacket private(override val channelID: Int,
                              override val targetIdentifier: String,
                              override val senderIdentifier: String,
                              header: String,
                              override val content: Array[Byte]) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"DataPacket{id: $channelID, header: $header, target: $targetIdentifier, sender: $senderIdentifier, content: ${new String(content)}}"


}

object DataPacket {

    def apply(header: String, content: Array[Byte] = Array())(implicit channel: PacketChannel): DataPacket =
        DataPacket(channel.channelID, channel.ownerIdentifier, channel.connectedIdentifier, header, content)

    def apply(header: String, content: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, content.getBytes)

    def apply(header: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, "")

    object Factory extends PacketFactory[DataPacket] {

        private val TYPE = "[data]".getBytes
        private val HEADER = "<header>".getBytes
        private val CONTENT = "<content>".getBytes

        override def toBytes(implicit packet: DataPacket): Array[Byte] = {
            val headerBytes = packet.header.getBytes
            TYPE ++ EmptyPacket.Factory.toBytesUnsigned(packet)
            HEADER ++ headerBytes ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): DataPacket = {
            val base = EmptyPacket.Factory.toPacket(bytes)
            val header = cutString(HEADER, CONTENT)
            val content = cutEnd(CONTENT)
            DataPacket(base.channelID, header, base.targetIdentifier, base.senderIdentifier, content)
        }

    }

}


