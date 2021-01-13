package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketCollector protected(traffic: PacketTraffic, identifier: Int)
        extends AbstractPacketCollector(traffic, identifier, true) with PacketCollector.Async {

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

    override def createNew(traffic: PacketTraffic, collectorId: Int): AsyncPacketCollector = {
        new AsyncPacketCollector(traffic, collectorId)
    }
}
