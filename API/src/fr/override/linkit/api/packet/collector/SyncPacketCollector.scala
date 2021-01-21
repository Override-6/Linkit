package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.concurency.PacketWorkerThread
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

class SyncPacketCollector(traffic: PacketTraffic, override val identifier: Int)
        extends AbstractPacketCollector(traffic, identifier, false) with PacketCollector.Sync {

    private val queue: BlockingDeque[(Packet, PacketCoordinates)] = new LinkedBlockingDeque()

    override def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        if (queue.isEmpty) {
            PacketWorkerThread.checkNotCurrent()
        }

        queue.takeLast().asInstanceOf[(P, PacketCoordinates)]
    }

    override def haveMorePackets: Boolean = !queue.isEmpty

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val element = (packet, coordinates)
        queue.addFirst(element)
    }
}

object SyncPacketCollector extends PacketCollectorFactory[SyncPacketCollector] {
    override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

    override def createNew(traffic: PacketTraffic, collectorId: Int): SyncPacketCollector = {
        new SyncPacketCollector(traffic, collectorId)
    }
}

