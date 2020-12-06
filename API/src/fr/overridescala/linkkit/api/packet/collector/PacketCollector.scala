package fr.overridescala.linkkit.api.packet.collector

import fr.overridescala.linkkit.api.packet.{Packet, PacketContainer, PacketCoordinates, TrafficHandler}
import fr.overridescala.linkkit.api.system.Reason

abstract class PacketCollector(handler: TrafficHandler) extends PacketContainer {

    handler.register(this)

    def sendPacket(packet: Packet, targetID: String): Unit = {
        handler.sendPacket(packet, identifier, targetID)
    }

    override def close(reason: Reason): Unit = handler.unregister(identifier, reason)


}

object PacketCollector {
    abstract class Sync(handler: TrafficHandler) extends PacketCollector(handler) {

        def nextPacket[P <: Packet](targetID: String, typeOfP: Class[P]): P

        def nextPacket[P <: Packet](typeOfP: Class[P]): P

        def nextPacketAndCoordinate[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates)

        def nextPacketAndCoordinate[P <: Packet](typeOfP: Class[P]): (Packet, PacketCoordinates)

        def haveMorePackets: Boolean

    }

    abstract class Async(traffic: TrafficHandler) extends PacketCollector(traffic) {

        def onPacketReceived(biConsumer: (Packet, PacketCoordinates) => Unit): Unit

    }

}
