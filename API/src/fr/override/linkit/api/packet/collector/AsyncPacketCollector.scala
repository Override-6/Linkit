package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.utils.AsyncExecutionContext.context

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketCollector(traffic: TrafficHandler,
                           override val identifier: Int) extends PacketCollector.Async(traffic) {

    @volatile private var onPacketReceivedAction: (Packet, PacketCoordinates) => Unit = _

    override def onPacketReceived(biConsumer: (Packet, PacketCoordinates) => Unit): Unit = {
        onPacketReceivedAction = biConsumer
    }

    override def sendPacket(packet: Packet, targetID: String): Unit = {
        //FIXME Future {
            try {
                traffic.sendPacket(packet, identifier, targetID)
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        //}(context)
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
