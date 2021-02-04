package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketCollector protected(traffic: PacketTraffic, identifier: Int)
        extends AbstractPacketCollector(traffic, identifier, true) with GlobalPacketSender with GlobalPacketAsyncReceiver {

    private val packetReceivedListeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    @relayWorkerExecution
    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        try {
            packetReceivedListeners.applyAll((packet, coordinates))
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
        }
    }

    override def sendPacket(packet: Packet, targetID: String): Unit = traffic.writePacket(packet, identifier, targetID)

    override def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedListeners += (tuple => callback(tuple._1, tuple._2))
    }
}

object AsyncPacketCollector extends PacketCollectorFactory[AsyncPacketCollector] {
    override val collectorClass: Class[AsyncPacketCollector] = classOf[AsyncPacketCollector]

    override def createNew(traffic: PacketTraffic, collectorId: Int): AsyncPacketCollector = {
        new AsyncPacketCollector(traffic, collectorId)
    }
}
