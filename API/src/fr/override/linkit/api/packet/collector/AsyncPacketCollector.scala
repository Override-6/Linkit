package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketSender
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketCollector protected(sender: PacketSender, identifier: Int)
        extends AbstractPacketCollector(sender, identifier) with PacketCollector.Async {

    private val packetReceivedListeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    override def onPacketInjected(biConsumer: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedListeners.add(tuple => biConsumer(tuple._1, tuple._2))
    }

    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        try {
            packetReceivedListeners.applyAll(packet, coordinates)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
        }
    }
}

object AsyncPacketCollector extends PacketCollectorFactory[AsyncPacketCollector] {
    override val collectorClass: Class[AsyncPacketCollector] = classOf[AsyncPacketCollector]

    override def createNew(sender: PacketSender, collectorId: Int): AsyncPacketCollector = {
        new AsyncPacketCollector(sender, collectorId)
    }
}
