package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.{HoleyPacketContainer, Packet, PacketContainer, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.system.CloseReason

abstract class PacketCollector(handler: TrafficHandler) extends PacketContainer {

    handler.register(this)

    def sendPacket(packet: Packet, targetID: String): Unit = {
        handler.sendPacket(packet, identifier, targetID)
    }

    override def close(reason: CloseReason): Unit = handler.unregister(identifier, reason)

}

object PacketCollector {

    abstract class Sync(handler: TrafficHandler) extends PacketCollector(handler) {

        def nextPacket[P <: Packet](targetID: String, typeOfP: Class[P]): P = nextPacketAndCoordinates(targetID, typeOfP)._1

        def nextPacket[P <: Packet](typeOfP: Class[P]): P = nextPacketAndCoordinates(typeOfP)._1

        def nextPacket(targetID: String): Packet = nextPacketAndCoordinates(targetID)._1

        def nextPacketAndCoordinates[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates)

        def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates)

        def nextPacketAndCoordinates(targetID: String): (Packet, PacketCoordinates) = nextPacketAndCoordinates(targetID, classOf[Packet])

        def haveMorePackets: Boolean

    }

    abstract class Async(traffic: TrafficHandler) extends PacketCollector(traffic) with HoleyPacketContainer

}
