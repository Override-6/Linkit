package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketCollector protected(writer: PacketWriter)
        extends AbstractPacketCollector(writer, true) with GlobalPacketSender with GlobalPacketAsyncReceiver {

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

    override def sendPacket(packet: Packet, targetID: String): Unit = writer.writePacket(packet, targetID)

    override def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedListeners += (tuple => callback(tuple._1, tuple._2))
    }
}

object AsyncPacketCollector extends PacketCollectorFactory[AsyncPacketCollector] {
    override def createNew(writer: PacketWriter): AsyncPacketCollector = {
        new AsyncPacketCollector(writer)
    }
}
