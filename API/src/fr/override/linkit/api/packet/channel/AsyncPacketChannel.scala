package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.traffic.PacketSender
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.AsyncExecutionContext.context

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel protected(override val connectedID: String,
                                   override val identifier: Int,
                                   sender: PacketSender) extends PacketChannel.Async(sender) {

    private var onPacketReceived: (Packet, PacketCoordinates) => Unit = _

    override def injectPacket(packet: Packet, ignored: PacketCoordinates): Unit = {
        Future {
            try {
                if (onPacketReceived != null)
                    onPacketReceived(packet, coordinates)
                //handler.notifyPacketUsed(packet, coordinates)
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    def onPacketInjected(consumer: (Packet, PacketCoordinates) => Unit): Unit = {
        onPacketReceived = consumer
    }

}

object AsyncPacketChannel extends PacketChannelFactory[AsyncPacketChannel] {
    override val channelClass: Class[AsyncPacketChannel] = classOf[AsyncPacketChannel]

    override def createNew(sender: PacketSender, channelId: Int, connectedID: String): AsyncPacketChannel = {
        new AsyncPacketChannel(connectedID, channelId, sender)
    }
}