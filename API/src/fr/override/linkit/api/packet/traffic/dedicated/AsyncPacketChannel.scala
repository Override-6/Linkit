package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.concurrency.{RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketAsyncReceiver, PacketInjectableFactory, PacketSender}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(scope: ChannelScope)
        extends AbstractPacketChannel(scope) with PacketSender with PacketAsyncReceiver {

    private val packetReceivedContainer: ConsumerContainer[(Packet, PacketCoordinates)] = ConsumerContainer()

    @relayWorkerExecution
    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        val pool = RelayWorkerThreadPool.currentThreadPool().get
        pool.runLater {
            try {
                packetReceivedContainer.applyAll((packet, coords))
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    override def send(packet: Packet): Unit = {
        scope.sendToAll(packet)
    }

    override def sendTo(target: String, packet: Packet): Unit = {
        scope.sendTo(target, packet)
    }

    override def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedContainer += (tuple => callback(tuple._1, tuple._2))
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(scope)
    }

}