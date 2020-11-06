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
class DataPacket(override val channelID: Int,
                 override val targetID: String,
                 override val senderID: String,
                 val header: String,
                 val content: Array[Byte]) extends Packet {

    val contentAsString: String = new String(content)

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"DataPacket{id: $channelID, header: $header, target: $targetID, sender: $senderID, content: ${new String(content)}}"

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

}

object DataPacket {

    def apply(header: String, content: Array[Byte] = Array())(implicit channel: PacketChannel): DataPacket =
        new DataPacket(channel.channelID, channel.connectedID, channel.ownerID, header, content)

    def apply(header: String, content: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, content.getBytes)

    def apply(header: String)(implicit channel: PacketChannel): DataPacket =
        apply(header, "")

    def apply(content: Array[Byte])(implicit channel: PacketChannel): DataPacket =
        apply("", content)

    def apply(targetID: String, header: String, content: Array[Byte])(implicit relay: Relay): DataPacket =
        new DataPacket(-1, targetID, relay.identifier, header, content)

    object Factory extends PacketFactory[DataPacket] {

        import fr.overridescala.vps.ftp.api.packet.ext.PacketUtils._

        private val TYPE = "[data]".getBytes
        private val CONTENT = "<content>".getBytes

        override def decompose(implicit packet: DataPacket): Array[Byte] = {
            val header = packet.header.getBytes
            TYPE ++ header ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.containsSlice(TYPE)

        override def build(channelID: Int, senderID: String, targetId: String)(implicit bytes: Array[Byte]): DataPacket = {
            val header = cutString(TYPE, CONTENT)
            val content = cutEnd(CONTENT)
            new DataPacket(channelID, targetId, senderID, header, content)
        }

    }

}


