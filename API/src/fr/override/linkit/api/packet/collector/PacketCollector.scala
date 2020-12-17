package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.packet.{Packet, PacketContainer, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.system.Reason

abstract class PacketCollector(handler: TrafficHandler) extends PacketContainer {

    handler.register(this)

    def sendPacket(packet: Packet, targetID: String): Unit = {
        handler.sendPacket(packet, identifier, targetID)
    }

    override def close(reason: Reason): Unit = handler.unregister(identifier, reason)


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

    abstract class Async(traffic: TrafficHandler) extends PacketCollector(traffic) {

        def onPacketReceived(biConsumer: (Packet, PacketCoordinates) => Unit): Unit

    }

}
