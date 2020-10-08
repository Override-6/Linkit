package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket

/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannelManager]]
 * @see [[DataPacket]]
 * */
trait PacketChannel {

    /**
     * Builds a [[DataPacket]] from a header string and a content byte array,
     * then send it to the targeted Relay that complete the task
     *
     * @param header the packet header
     * @param content the packet content
     * */
    def sendPacket(header: String, content: Array[Byte]): Unit

    /**
     * Builds a [[DataPacket]] from a header string and a content string,
     * then send it to the targeted Relay that complete the task
     *
     * @param header the packet header
     * @param content the packet content
     * */
    def sendPacket(header: String, content: String): Unit =
        sendPacket(header, content.getBytes)

    /**
     * Builds a [[DataPacket]] from a header string and no content (content is empty),
     * then send it to the targeted Relay that complete the task
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
     * Targets a event when a specified packet with the targeted header is received.
     * @param uses the number of time the event can be fired
     * @param header the header to target.
     * @param onReceived the event to call
     * */
    def putListener(header: String, onReceived: DataPacket => Unit, uses: Int = 1, enqueuePacket: Boolean = false): Unit

    /**
     * de-register a event
     * @param header the packet header to stop listening.
     * */
    def removeListener(header: String)

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}
