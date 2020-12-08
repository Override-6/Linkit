package fr.overridescala.linkkit.api.packet.collector

import fr.overridescala.linkkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.overridescala.linkkit.api.utils.async

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketCollector(traffic: TrafficHandler,
                           override val identifier: Int) extends PacketCollector.Async(traffic) {

    @volatile private var onPacketReceivedAction: (Packet, PacketCoordinates) => Unit = _

    override def onPacketReceived(biConsumer: (Packet, PacketCoordinates) => Unit): Unit = {
        onPacketReceivedAction = biConsumer
    }

    override def sendPacket(packet: Packet, targetID: String): Unit = {
        Future {
            traffic.sendPacket(packet, identifier, targetID)
        }
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        try {
            if (onPacketReceivedAction != null)
                onPacketReceivedAction(packet, coordinates)
            //handler.notifyPacketUsed(packet, coordinates)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
        }
    }
}