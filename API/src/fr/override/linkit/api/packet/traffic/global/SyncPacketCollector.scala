package fr.`override`.linkit.api.packet.traffic.global

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool}
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

class SyncPacketCollector(writer: PacketWriter, providable: Boolean)
        extends AbstractPacketCollector(writer, false) with GlobalPacketSender with GlobalPacketSyncReceiver {

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

    override def sendPacket(packet: Packet, targetID: String): Unit = writer.writePacket(packet, targetID)
}

object SyncPacketCollector extends PacketCollectorFactory[SyncPacketCollector] {

    override def createNew(writer: PacketWriter): SyncPacketCollector = {
        new SyncPacketCollector(writer, false)
    }

    def providable: PacketCollectorFactory[SyncPacketCollector] = {
        new SyncPacketCollector(_, true)
    }
}

