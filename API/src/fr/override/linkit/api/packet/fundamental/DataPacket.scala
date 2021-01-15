package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator}

//TODO Doc
/**
 * this class is used to represent a packet to send or to receive.
 * It allows the user and the program to work easier with the packets.
 * a DataPacket can only be send into tasks
 *
 * @param header  the header of the packet, or the type of this data. Headers allows to classify packets / data to send or receive
 * @param content the content of this packet. can be an [[Object]], a [[String]] or whatever. default content is empty
 * */
case class DataPacket(header: String,
                      content: Array[Byte] = Array()) extends Packet {

    lazy val contentAsString: String = new String(content)

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"DataPacket(header: $header, content: ${new String(content)})"

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

}

object DataPacket extends PacketFactory[DataPacket] {

    def apply(header: String, content: Array[Byte] = Array()): DataPacket =
        new DataPacket(header, content)

    def apply(header: String, content: String): DataPacket =
        apply(header, content.getBytes)

    def apply(header: String): DataPacket =
        apply(header, "")

    def apply(content: Array[Byte]): DataPacket =
        apply("", content)

    import fr.`override`.linkit.api.packet.PacketUtils._

    private val TYPE = "[data]".getBytes
    private val CONTENT = "<content>".getBytes

    override def decompose(translator: PacketTranslator)(implicit packet: DataPacket): Array[Byte] = {
        val header = packet.header.getBytes
        TYPE ++ header ++
                CONTENT ++ packet.content
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean =
        bytes.startsWith(TYPE)

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): DataPacket = {
        val header = stringBetween(TYPE, CONTENT)
        val content = untilEnd(CONTENT)
        new DataPacket(header, content)
    }

    override val packetClass: Class[DataPacket] = classOf[DataPacket]
}


