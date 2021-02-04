package fr.`override`.linkit.api.packet.traffic.dedicated

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.dedicated.SyncPacketChannel.SyncPacketChannelBehavior
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason


//TODO doc
class SyncPacketChannel protected(traffic: PacketTraffic,
                                  connectedID: String,
                                  identifier: Int,
                                  behavior: SyncPacketChannelBehavior)
        extends AbstractPacketChannel(connectedID, identifier, traffic) with DedicatedPacketSender with DedicatedPacketSyncReceiver {


    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[Packet] = {
        if (!behavior.providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool
                    .ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    /**
     * add a packet into the PacketChannel. the PacketChannel will stop waiting in [[PacketChannel#nextPacket]] if it where waiting for a packet
     *
     * @param packet the packet to add
     * @param coords the packet coordinates
     *
     * @throws IllegalArgumentException if the packet coordinates does not match with this channel coordinates
     * */
    @relayWorkerExecution
    override def injectPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        if (coords.senderID != connectedID)
            throw new UnexpectedPacketException("Attempted to inject a packet that comes from a relay that is not bound to this channel")
        queue.add(packet)
    }

    override def close(reason: CloseReason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override def nextPacket(): Packet = {
        if (queue.isEmpty)
            PacketWorkerThread.checkNotCurrent()
        val packet = queue.take()
        packet
    }


    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] would not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty

    override def sendPacket(packet: Packet): Unit = traffic.writePacket(packet, coordinates)
}


object SyncPacketChannel extends PacketChannelFactory[SyncPacketChannel] {
    override type T = SyncPacketChannelBehavior

    private val DefaultBehavior: SyncPacketChannelBehavior = SyncPacketChannelBehavior()

    override val channelClass: Class[SyncPacketChannel] = classOf[SyncPacketChannel]

    override def createNew(traffic: PacketTraffic, collectorId: Int, connectedID: String): SyncPacketChannel = {
        new SyncPacketChannel(traffic, connectedID, collectorId, DefaultBehavior)
    }

    override implicit def behavioralFactory(behavior: SyncPacketChannelBehavior): PacketChannelFactory[SyncPacketChannel] = {
        new PacketChannelFactory[SyncPacketChannel] {
            override val channelClass: Class[SyncPacketChannel] = classOf[SyncPacketChannel]

            override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): SyncPacketChannel = {
                new SyncPacketChannel(traffic, connectedID, channelId, behavior)
            }
        }
    }

    def apply: SyncPacketChannelBehavior = SyncPacketChannelBehavior()

    case class SyncPacketChannelBehavior() extends ChannelBehavior {
        private[SyncPacketChannel] var providable = false
        private[SyncPacketChannel] var overlay: Packet => Packet = packet => packet

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
