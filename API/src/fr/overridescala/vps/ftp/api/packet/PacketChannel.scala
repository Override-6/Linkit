package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket

/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannelManager]]
 * @see [[DataPacket]]
 * */
trait PacketChannel extends AutoCloseable {

    val ownerIdentifier: String
    val connectedIdentifier: String
    val channelID: Int

    /**
     * send any packet to the connected Relay
     *
     * @param packet the packet to send
     * @throws UnexpectedPacketException if the packet is an instance of [[TaskInitPacket]]
     * */
    def sendPacket[P <: Packet](packet: P): Unit

    /**
     * Waits until a data packet is received and concerned about this task.
     *
     * @return the received packet
     * @see [[Packet]]
     * */

    def nextPacket(): Packet

    def nextPacketAsP[P <: Packet](): P =
        nextPacket().asInstanceOf[P]


    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean

    //TODO create two kind of PacketChannels, ConcurrentPacketChannel, and AsyncPacketChannel.
    /**
     * @param event returns true if the packet must be enqueued,
     * */
    def setOnPacketAdded(event: Packet => Boolean): Unit

}