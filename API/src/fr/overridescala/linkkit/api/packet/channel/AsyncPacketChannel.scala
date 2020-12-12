package fr.overridescala.linkkit.api.packet.channel

import fr.overridescala.linkkit.api.exception.PacketException
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket
import fr.overridescala.linkkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel(override val connectedID: String,
                         override val identifier: Int,
                         traffic: TrafficHandler) extends PacketChannel.Async(traffic) {

    private var onPacketReceived: Packet => Unit = _

    override def sendPacket(packet: Packet): Unit = {
        traffic.sendPacket(packet, coordinates)
    }

    override def injectPacket(packet: Packet, ignored: PacketCoordinates): Unit = {
        Future {
            try {
                if (onPacketReceived != null)
                    onPacketReceived(packet)
                //handler.notifyPacketUsed(packet, coordinates)
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    def onPacketReceived(consumer: Packet => Unit): Unit = {
        onPacketReceived = consumer
    }

}
