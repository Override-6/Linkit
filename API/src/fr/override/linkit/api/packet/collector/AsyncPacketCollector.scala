package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketCollector protected(sender: PacketWriter, identifier: Int)
        extends AbstractPacketCollector(sender, identifier, true) with PacketCollector.Async {

    private val packetReceivedListeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        try {
            packetReceivedListeners.applyAll((packet, coordinates))
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
        }
    }

    override def addOnPacketInjected(biConsumer: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedListeners += (tuple => biConsumer(tuple._1, tuple._2))
    }
}

object AsyncPacketCollector extends PacketCollectorFactory[AsyncPacketCollector] {
    override val collectorClass: Class[AsyncPacketCollector] = classOf[AsyncPacketCollector]

    override def createNew(writer: PacketWriter, collectorId: Int): AsyncPacketCollector = {
        new AsyncPacketCollector(writer, collectorId)
    }
}
