package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.concurrency.SyncExecutionContext
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel protected(override val connectedID: String,
                                   override val identifier: Int,
                                   traffic: PacketTraffic) extends PacketChannel.Async(traffic) {

    private val packetReceivedContainer: ConsumerContainer[(Packet, PacketCoordinates)] = ConsumerContainer()

    override def injectPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        Future {
            try {
                packetReceivedContainer.applyAll((packet, coords))
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }(SyncExecutionContext)
    }

    def addOnPacketInjected(consumer: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedContainer += (tuple => consumer(tuple._1, tuple._2))
    }

}

object AsyncPacketChannel extends PacketChannelFactory[AsyncPacketChannel] {
    override val channelClass: Class[AsyncPacketChannel] = classOf[AsyncPacketChannel]

    override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): AsyncPacketChannel = {
        new AsyncPacketChannel(connectedID, channelId, traffic)
    }
}