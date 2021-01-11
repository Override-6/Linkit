package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

class SyncPacketCollector(sender: PacketWriter, override val identifier: Int)
        extends AbstractPacketCollector(sender, identifier, false) with PacketCollector.Sync {

    private val queue: BlockingDeque[(Packet, PacketCoordinates)] = new LinkedBlockingDeque()

    override def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates) = {
        if (queue.isEmpty) {
            sender.checkThread()
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

    override def createNew(writer: PacketWriter, collectorId: Int): SyncPacketCollector = {
        new SyncPacketCollector(writer, collectorId)
    }
}

