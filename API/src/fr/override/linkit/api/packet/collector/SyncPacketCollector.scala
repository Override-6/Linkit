package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

class SyncPacketCollector(traffic: PacketTraffic,
                          override val identifier: Int,
                          providable: Boolean)
        extends AbstractPacketCollector(traffic, identifier, false) with PacketCollector.Sync {

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[(Packet, PacketCoordinates)] = {
        if (!providable)
            new LinkedBlockingQueue[(Packet, PacketCoordinates)]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[(Packet, PacketCoordinates)]())
        }
    }

    override def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        if (queue.isEmpty) {
            PacketWorkerThread.checkNotCurrent()
        }

        queue.take().asInstanceOf[(P, PacketCoordinates)]
    }

    override def haveMorePackets: Boolean = !queue.isEmpty

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val element = (packet, coordinates)
        queue.add(element)
    }
}

object SyncPacketCollector extends PacketCollectorFactory[SyncPacketCollector] {
    override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

    private val providableFactory: PacketCollectorFactory[SyncPacketCollector] = new PacketCollectorFactory[SyncPacketCollector] {
        override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

        override def createNew(traffic: PacketTraffic, channelId: Int): SyncPacketCollector = {
            new SyncPacketCollector(traffic, channelId, true)
        }
    }

    override def createNew(traffic: PacketTraffic, collectorId: Int): SyncPacketCollector = {
        new SyncPacketCollector(traffic, collectorId, false)
    }

    def providable: PacketCollectorFactory[SyncPacketCollector] = providableFactory

}
