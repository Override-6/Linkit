package fr.overridescala.linkkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.overridescala.linkkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.overridescala.linkkit.api.system.Reason

import scala.collection.mutable

class SyncPacketCollector(handler: TrafficHandler,
                          override val identifier: Int) extends PacketCollector.Sync(handler) {

    private val categorisedQueue: mutable.Map[String, BlockingDeque[(Packet, PacketCoordinates)]] = mutable.Map.empty
    private val rawQueue: BlockingDeque[(Packet, PacketCoordinates)] = new LinkedBlockingDeque()

    override def nextPacket[P <: Packet](targetID: String, typeOfP: Class[P]): P = {
        nextPacketAndCoordinate(targetID, typeOfP)._1
    }

    override def nextPacket[P <: Packet](typeOfP: Class[P]): P = {
        nextPacketAndCoordinate(typeOfP)._1
    }

    override def nextPacketAndCoordinate[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        val element = rawQueue.takeLast() //TODO event processing
        val senderID = element._2.senderID

        categorisedQueue(senderID).remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def nextPacketAndCoordinate[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates) = {
        val queue = categorisedQueue.getOrElseUpdate(targetID, new LinkedBlockingDeque)

        val element = queue.takeLast() //TODO event processing
        rawQueue.remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def haveMorePackets: Boolean = !rawQueue.isEmpty

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val element = (packet, coordinates)
        rawQueue.addFirst(element)
        categorisedQueue.getOrElseUpdate(coordinates.senderID, new LinkedBlockingDeque).addFirst(element)

    }

    override def close(reason: Reason): Unit = {
        super.close(reason)
        categorisedQueue.clear()
        rawQueue.clear()
    }

}
