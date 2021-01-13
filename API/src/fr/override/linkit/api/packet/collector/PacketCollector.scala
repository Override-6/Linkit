package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.{ImmediatePacketInjectable, PacketInjectable}
import fr.`override`.linkit.api.system.CloseReason

trait PacketCollector extends PacketInjectable {

    override val ownerID: String

    override def close(reason: CloseReason): Unit

    override def isClosed: Boolean

    def sendPacket(packet: Packet, targetID: String): Unit

    def broadcastPacket(packet: Packet)

    /**
     * Creates a PacketChannel that will be handled by this packet collector. <br>
     * The packet channel can send and receive packets from the target.  <br>
     * In other words, this will create a sub channel that is bound to a specific relay.  <br>
     * If the sub channel receives a packet, the parent collector will handle the packet as well if the
     * children authorises it
     *
     * @param targetID the bounded relay identifier that the sub channel will focus
     * @param transparent if ture, the children is set as 'transparent',
     *                    that means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * @param factory the factory that wil determine the kind of PacketChannel that will be used.
     * */
    def subChannel[C <: PacketChannel](targetID: String, factory: PacketChannelFactory[C], transparent: Boolean): C

}

object PacketCollector {

    trait Async extends PacketCollector with ImmediatePacketInjectable

    trait Sync extends PacketCollector {

        def nextPacket[P <: Packet](typeOfP: Class[P]): P = nextPacketAndCoordinates(typeOfP)._1

        def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates)

        def haveMorePackets: Boolean

    }

}
