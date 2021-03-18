package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.`override`.linkit.core.local.concurrency.BusyWorkerThread
import fr.`override`.linkit.core.local.utils.ConsumerContainer
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketAsyncReceiver, PacketInjectableFactory, PacketSender}
import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.core.connection.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.core.local.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(scope: ChannelScope)
        extends AbstractPacketChannel(scope) with PacketSender with PacketAsyncReceiver {

    private val packetReceivedContainer: ConsumerContainer[(Packet, DedicatedPacketCoordinates)] = ConsumerContainer()

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val pool = BusyWorkerThread.currentPool().get
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