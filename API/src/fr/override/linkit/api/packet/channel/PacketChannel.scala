package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.traffic.{ImmediatePacketInjectable, PacketInjectable, PacketWriter}
import fr.`override`.linkit.api.packet.{PacketFactory, _}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

//TODO Think about on creating an abstract class to implement this class as a trait
/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannel]]
 * */
abstract class PacketChannel(sender: PacketWriter) extends JustifiedCloseable with PacketInjectable {

    override val ownerID: String = sender.ownerID
    @volatile private var closed = false
    val connectedID: String
    val coordinates: PacketCoordinates = PacketCoordinates(identifier, connectedID, ownerID)

    override def close(reason: CloseReason): Unit = {
        closed = true
    }

    override def isClosed: Boolean = closed

    def sendPacket(packet: Packet): Unit = sender.writePacket(packet, coordinates)

}

object PacketChannel {

    abstract class Async(handler: PacketWriter) extends PacketChannel(handler) with ImmediatePacketInjectable

    abstract class Sync(traffic: PacketWriter) extends PacketChannel(traffic) {

        /**
         * Waits until a data packet is received and concerned about this task.
         *
         * @return the received packet
         * @see [[DataPacket]]
         * */
        def nextPacket(): Packet

        def nextPacketAsP[P <: Packet](): P = nextPacket().asInstanceOf[P]

        def nextPacket[P <: Packet](classOfP: Class[P]): P = nextPacketAsP()

        def nextPacket[P <: Packet](factoryOfP: PacketFactory[P]): P = nextPacket(factoryOfP.packetClass)

        /**
         * @return true if this channel contains stored packets. In other words, return true if [[nextPacketAsP]] will not wait
         * */
        def haveMorePackets: Boolean
    }

}