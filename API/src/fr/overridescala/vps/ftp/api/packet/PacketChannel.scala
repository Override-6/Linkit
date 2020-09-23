package fr.overridescala.vps.ftp.api.packet

/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannelManager]]
 * @see [[DataPacket]]
 * */
trait PacketChannel {

    /**
     * Builds a [[DataPacket]] from a header string and a content byte array.
     *
     * @param header the packet header
     * @param content the packet content
     * */
    def sendPacket(header: String, content: Array[Byte]): Unit

    /**
     * Builds a [[DataPacket]] from a header string and a content string.
     *
     * @param header the packet header
     * @param content the packet content
     * */
    def sendPacket(header: String, content: String): Unit =
        sendPacket(header, content.getBytes)

    /**
     * Builds a [[DataPacket]] from a header string and no content. (content is empty)
     *
     * @param header the packet header
     * */
    def sendPacket(header: String): Unit =
        sendPacket(header, "")

    /**
     * Waits until a data packet is received and concerned about this task.
     *
     * @return the received packet
     * @see [[DataPacket]]
     * */
    def nextPacket(): DataPacket

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}
