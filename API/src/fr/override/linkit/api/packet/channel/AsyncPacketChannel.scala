package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, SyncExecutionContext}

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncPacketChannel protected(override val connectedID: String,
                                   override val identifier: Int,
                                   sender: PacketWriter) extends PacketChannel.Async(sender) {

    private val packetReceivedContainer: ConsumerContainer[(Packet, PacketCoordinates)] = ConsumerContainer()

    override def injectPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        if (coords.senderID != connectedID)
            throw new UnexpectedPacketException("Attempted to inject a packet that comes from a relay that is not bound to this channel")
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

    override def createNew(writer: PacketWriter, channelId: Int, connectedID: String): AsyncPacketChannel = {
        new AsyncPacketChannel(connectedID, channelId, writer)
    }
}