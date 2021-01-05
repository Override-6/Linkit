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

    def subChannel[C <: PacketChannel](boundIdentifier: String, factory: PacketChannelFactory[C]): C

}

object PacketCollector {

    trait Async extends PacketCollector with ImmediatePacketInjectable

    trait Sync extends PacketCollector {

        def nextPacket[P <: Packet](targetID: String, typeOfP: Class[P]): P = nextPacketAndCoordinates(targetID, typeOfP)._1

        def nextPacket[P <: Packet](typeOfP: Class[P]): P = nextPacketAndCoordinates(typeOfP)._1

        def nextPacket(targetID: String): Packet = nextPacketAndCoordinates(targetID)._1

        def nextPacketAndCoordinates[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates)

        def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates)

        def nextPacketAndCoordinates(targetID: String): (Packet, PacketCoordinates) = nextPacketAndCoordinates(targetID, classOf[Packet])

        def haveMorePackets: Boolean

    }

}
