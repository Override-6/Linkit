package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.concurrency.{RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(writer: PacketWriter,
                                   connectedID: String)

        extends AbstractPacketChannel(writer, connectedID) with DedicatedPacketSender with DedicatedPacketAsyncReceiver {

    private val packetReceivedContainer: ConsumerContainer[Packet] = ConsumerContainer()

    @relayWorkerExecution
    override def injectPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        val pool = RelayWorkerThreadPool.currentThreadPool().get
        pool.runLater {
            try {
                packetReceivedContainer.applyAll(packet)
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    override def sendPacket(packet: Packet): Unit = {
        writer.writePacket(packet, connectedID)
    }

    override def onPacketReceived(callback: Packet => Unit): Unit = {
        packetReceivedContainer += callback
    }
}

object AsyncPacketChannel extends PacketChannelFactory[AsyncPacketChannel] {

    override def createNew(writer: PacketWriter, connectedID: String): AsyncPacketChannel = {
        new AsyncPacketChannel(writer, connectedID)
    }

}