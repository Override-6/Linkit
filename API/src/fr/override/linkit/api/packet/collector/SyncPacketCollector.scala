package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.PacketSender
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable

class SyncPacketCollector(sender: PacketSender, override val identifier: Int)
        extends AbstractPacketCollector(sender, identifier) with PacketCollector.Sync {

    private val categorisedQueue: mutable.Map[String, BlockingDeque[(Packet, PacketCoordinates)]] = mutable.Map.empty
    private val rawQueue: BlockingDeque[(Packet, PacketCoordinates)] = new LinkedBlockingDeque()

    override def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        if (rawQueue.isEmpty) {
            sender.checkThread()
        }

        val element = rawQueue.takeLast() //TODO event processing
        val senderID = element._2.senderID

        val opt = categorisedQueue.get(senderID)
        if (opt.isDefined)
            opt.get.remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def nextPacketAndCoordinates[P <: Packet](targetID: String, typeOfP: Class[P]): (P, PacketCoordinates) = {
        val queue = categorisedQueue.getOrElseUpdate(targetID, new LinkedBlockingDeque())

        if (queue.isEmpty) {
            sender.checkThread()
        }

        val element = queue.takeLast() //TODO event processing
        rawQueue.remove(element)
        element.asInstanceOf[(P, PacketCoordinates)]
    }

    override def haveMorePackets: Boolean = !rawQueue.isEmpty

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        categorisedQueue.clear()
        rawQueue.clear()
    }

    override protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val element = (packet, coordinates)

        rawQueue.addFirst(element)
        categorisedQueue.getOrElseUpdate(coordinates.senderID, new LinkedBlockingDeque())
                .addFirst(element)
    }
}

object SyncPacketCollector extends PacketCollectorFactory[SyncPacketCollector] {
    override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

    override def createNew(sender: PacketSender, collectorId: Int): SyncPacketCollector = {
        new SyncPacketCollector(sender, collectorId)
    }
}

