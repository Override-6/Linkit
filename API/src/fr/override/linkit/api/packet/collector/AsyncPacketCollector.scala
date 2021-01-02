package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.channel.PacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class AsyncPacketCollector(traffic: TrafficHandler,
                           override val identifier: Int) extends PacketCollector.Async(traffic) {

    private val packetReceivedListeners = ConsumerContainer[(Packet, PacketCoordinates)]()
    private val subChannels = ListBuffer.empty[PacketChannel.Async]

    override def onPacketInjected(biConsumer: (Packet, PacketCoordinates) => Unit): Unit = {
        packetReceivedListeners.add(tuple => biConsumer(tuple._1, tuple._2))
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
            packetReceivedListeners.applyAll(packet, coordinates)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
        }
    }

    override def subSyncChannel(boundIdentifier: String): PacketChannel = ???

    override def subAsyncChannel(boundIdentifier: String): PacketChannel = ???
}
