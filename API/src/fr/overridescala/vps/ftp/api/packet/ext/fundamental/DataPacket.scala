package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.Relay
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
case class DataPacket(override val channelID: Int,
                      override val targetID: String,
                      override val senderID: String,
                      header: String,
                      override val content: Array[Byte]) extends Packet{

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"DataPacket{id: $channelID, header: $header, target: $targetID, sender: $senderID, content: ${new String(content)}}"


}

object DataPacket {

    def apply(header: String, content: Array[Byte] = Array())(implicit channel: PacketChannel): DataPacket =
        DataPacket(channel.channelID, channel.connectedIdentifier, channel.ownerIdentifier, header, content)

    def apply(header: String, content: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, content.getBytes)

    def apply(header: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, "")

    def apply(content: Array[Byte])(implicit channel: PacketChannel): DataPacket =
        apply("", content)

    def apply(targetID: String, header: String, content: Array[Byte])(implicit relay: Relay): DataPacket =
        DataPacket(-1, targetID, relay.identifier, header, content)

    object Factory extends PacketFactory[DataPacket] {

        private val TYPE = "[data]".getBytes
        private val SENDER = "<sender>".getBytes
        private val TARGET = "<target>".getBytes
        private val HEADER = "<header>".getBytes
        private val CONTENT = "<content>".getBytes

        override def decompose(implicit packet: DataPacket): Array[Byte] = {
            val channelID = packet.channelID.toString.getBytes
            val sender = packet.senderID.getBytes
            val target = packet.targetID.getBytes
            val header = packet.header.getBytes
            TYPE ++ channelID ++
                    SENDER ++ sender ++
                    TARGET ++ target ++
                    HEADER ++ header ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def build(implicit bytes: Array[Byte]): DataPacket = {
            val channelID = cutString(TYPE, SENDER).toInt
            val sender = cutString(SENDER, TARGET)
            val target = cutString(TARGET, HEADER)
            val header = cutString(HEADER, CONTENT)
            val content = cutEnd(CONTENT)
            new DataPacket(channelID, target, sender, header, content)
        }

    }

}


