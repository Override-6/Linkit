package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.RelayCloseable
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket

//TODO Doc
/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannelManager]]
 * @see [[DataPacket]]
 * */
trait PacketChannel extends RelayCloseable {

    val ownerID: String
    val connectedID: String
    val channelID: Int

    def sendPacket[P <: Packet](packet: P): Unit

}

object PacketChannel {

    trait Async extends PacketChannel {
        def setOnPacketReceived(event: Packet => Unit): Unit
    }

    trait Sync extends PacketChannel {
        /**
         * Waits until a data packet is received and concerned about this task.
         *
         * @return the received packet
         * @see [[DataPacket]]
         * */
        def nextPacket(): Packet

        def nextPacketAsP[P <: Packet](): P

        /**
         * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
         * */
        def haveMorePackets: Boolean
    }

}