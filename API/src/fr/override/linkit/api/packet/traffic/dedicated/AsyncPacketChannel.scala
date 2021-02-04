package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.concurrency.{RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.dedicated.AsyncPacketChannel.AsyncPacketChannelBehavior
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(traffic: PacketTraffic,
                                   connectedID: String,
                                   identifier: Int,
                                   behaviour: AsyncPacketChannelBehavior)
        extends AbstractPacketChannel(connectedID, identifier, traffic) with DedicatedPacketSender with DedicatedPacketAsyncReceiver {

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
        traffic.writePacket(packet, coordinates)
    }

    override def onPacketReceived(callback: Packet => Unit): Unit = {
        packetReceivedContainer += callback
    }
}

object AsyncPacketChannel extends PacketChannelFactory[AsyncPacketChannel] {
    override type T = AsyncPacketChannelBehavior

    private val DefaultBehavior: AsyncPacketChannelBehavior = AsyncPacketChannelBehavior()

    override val channelClass: Class[AsyncPacketChannel] = classOf[AsyncPacketChannel]

    override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): AsyncPacketChannel = {
        new AsyncPacketChannel(traffic, connectedID, channelId, DefaultBehavior)
    }

    override implicit def behavioralFactory(behavior: AsyncPacketChannelBehavior): PacketChannelFactory[AsyncPacketChannel] = {
        new PacketChannelFactory[AsyncPacketChannel] {
            override val channelClass: Class[AsyncPacketChannel] = classOf[AsyncPacketChannel]

            override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): AsyncPacketChannel = {
                new AsyncPacketChannel(traffic, connectedID, channelId, behavior)
            }
        }
    }

    def apply: AsyncPacketChannelBehavior = AsyncPacketChannelBehavior()

    case class AsyncPacketChannelBehavior() extends ChannelBehavior {
        private[AsyncPacketChannel] var overlay: Packet => Packet = packet => packet

        def withOverlay(overlay: Packet => Packet): this.type = {
            this.overlay = overlay
            this
        }
    }

}