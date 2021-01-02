package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.utils.AsyncExecutionContext.context

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel(override val connectedID: String,
                         override val identifier: Int,
                         traffic: TrafficHandler) extends PacketChannel.Async(traffic) {

    private var onPacketReceived: (Packet, PacketCoordinates) => Unit = _

    override def sendPacket(packet: Packet): Unit = {
        traffic.sendPacket(packet, coordinates)
    }

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
