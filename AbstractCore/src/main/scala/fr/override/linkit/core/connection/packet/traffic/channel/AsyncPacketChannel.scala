package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.internal.concurrency.{RelayThreadPool, relayWorkerExecution}
import .PacketInjection
import fr.`override`.linkit.skull.connection.packet.traffic.{ChannelScope, PacketAsyncReceiver, PacketInjectableFactory, PacketSender}
import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.internal.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(scope: ChannelScope)
        extends AbstractPacketChannel(scope) with PacketSender with PacketAsyncReceiver {

    private val packetReceivedContainer: ConsumerContainer[(Packet, DedicatedPacketCoordinates)] = ConsumerContainer()

    @relayWorkerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val pool = RelayThreadPool.currentPool().get
        pool.runLater {
            try {
                val packets = injection.getPackets
                val coords = injection.coordinates
                packets.foreach(packet => packetReceivedContainer.applyAll((packet, coords)))
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    override def send(packet: Packet): Unit = {
        scope.sendToAll(packet)
    }

    override def sendTo(packet: Packet, targets: String*): Unit = {
        scope.sendTo(packet, targets: _*)
    }

    override def addOnPacketReceived(callback: (Packet, DedicatedPacketCoordinates) => Unit): Unit = {
        packetReceivedContainer += (tuple => callback(tuple._1, tuple._2))
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(scope)
    }

}