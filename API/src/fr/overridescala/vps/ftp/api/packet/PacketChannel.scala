package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.system.RelayCloseable
import fr.overridescala.vps.ftp.api.system.Reason

//TODO Doc
/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannelManager]]
 * @see [[DataPacket]]
 * */
abstract class PacketChannel(handler: PacketChannelsHandler) extends RelayCloseable {

    val ownerID: String
    val connectedID: String
    val channelID: Int


    override def close(reason: Reason): Unit = handler.unregisterManager(channelID, reason)

}

object PacketChannel {

    abstract class Async(handler: PacketChannelsHandler) extends PacketChannel(handler) {
        def sendPacket[P <: Packet](packet: P): Unit = handler.sendPacket(packet)

        def setOnPacketReceived(event: Packet => Unit): Unit
    }

    abstract class Sync(handler: PacketChannelsHandler) extends PacketChannel(handler) {
        def sendPacket[P <: Packet](packet: P): Unit = handler.sendPacket(packet)

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