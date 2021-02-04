package fr.`override`.linkit.api.packet.traffic.global

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.global.SyncPacketCollector.SyncPacketCollectorBehavior
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

class SyncPacketCollector(traffic: PacketTraffic,
                          override val identifier: Int,
                          behavior: SyncPacketCollectorBehavior)
        extends AbstractPacketCollector(traffic, identifier, false) with GlobalPacketSender with GlobalPacketSyncReceiver {

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[(Packet, PacketCoordinates)] = {
        if (!behavior.providable)
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

    override def sendPacket(packet: Packet, targetID: String): Unit = traffic.writePacket(packet, identifier, targetID)
}

object SyncPacketCollector extends PacketCollectorFactory[SyncPacketCollector] {
    override type T = SyncPacketCollectorBehavior

    private val DefaultBehavior: SyncPacketCollectorBehavior = SyncPacketCollectorBehavior()

    override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

    override def createNew(traffic: PacketTraffic, collectorId: Int): SyncPacketCollector = {
        new SyncPacketCollector(traffic, collectorId, DefaultBehavior)
    }

    override implicit def behavioralFactory(behavior: SyncPacketCollectorBehavior): PacketCollectorFactory[SyncPacketCollector] = {
        new PacketCollectorFactory[SyncPacketCollector] {
            override val collectorClass: Class[SyncPacketCollector] = classOf[SyncPacketCollector]

            override def createNew(traffic: PacketTraffic, channelId: Int): SyncPacketCollector = {
                new SyncPacketCollector(traffic, channelId, behavior)
            }
        }
    }

    def apply: SyncPacketCollectorBehavior = SyncPacketCollectorBehavior()

    case class SyncPacketCollectorBehavior() extends CollectorBehavior {
        private[SyncPacketCollector] var providable = false
        private[SyncPacketCollector] var overlay: Packet => Packet = packet => packet

        def asProvidable: this.type = {
            providable = true
            this
        }

        def withOverlay(overlay: Packet => Packet): this.type = {
            this.overlay = overlay
            this
        }
    }
}
