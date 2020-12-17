package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.system.Reason

import scala.collection.mutable

class SyncPacketCollector(handler: TrafficHandler,
                          packetCacheSize: Int,
                          override val identifier: Int) extends PacketCollector.Sync(handler) {

    private val categorisedQueue: mutable.Map[String, BlockingDeque[(Packet, PacketCoordinates)]] = mutable.Map.empty
    private val rawQueue: BlockingDeque[(Packet, PacketCoordinates)] = new LinkedBlockingDeque(packetCacheSize)

    override def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        if (rawQueue.isEmpty) {
            handler.checkThread()
        }

        val element = rawQueue.takeLast() //TODO event processing
        val senderID = element._2.senderID

        categorisedQueue(senderID).remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def nextPacketAndCoordinates[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates) = {
        val queue = categorisedQueue.getOrElseUpdate(targetID, new LinkedBlockingDeque(packetCacheSize))

        if (queue.isEmpty) {
            handler.checkThread()
        }

        val element = queue.takeLast() //TODO event processing
        rawQueue.remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def haveMorePackets: Boolean = !rawQueue.isEmpty

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val element = (packet, coordinates)

        rawQueue.addFirst(element)
        categorisedQueue.getOrElseUpdate(coordinates.senderID, new LinkedBlockingDeque(packetCacheSize)).addFirst(element)

    }

    override def close(reason: Reason): Unit = {
        super.close(reason)
        categorisedQueue.clear()
        rawQueue.clear()
    }

}
