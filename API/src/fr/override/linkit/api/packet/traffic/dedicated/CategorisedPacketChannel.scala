package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable

class CategorisedPacketChannel(connectedID: String,
                               identifier: Int,
                               traffic: PacketTraffic) extends AbstractPacketChannel(connectedID, identifier, traffic) {
    private val categories = mutable.Map.empty[String, PacketInjectable]

    @relayWorkerExecution
    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        packet match {
            case WrappedPacket(category, subPacket) =>
                categories.get(category).foreach(_.injectPacket(packet, coordinates))
        }
    }
    //TODO
/*
    class SyncCategory(name: String) extends SyncPacketChannel(traffic, connectedID, identifier, new SyncPacketChannelBehavior(true, WrappedPacket(name, _))) {
    }

    class AsyncCategory(name: String) extends AsyncPacketChannel(traffic, connectedID, identifier, new AsyncPacketChannelBehavior(WrappedPacket(name, _)))
            with DedicatedPacketAsyncReceiver with DedicatedPacketSender {
        override def onPacketReceived(callback: Packet => Unit): Unit = ???

        override def sendPacket(packet: Packet): Unit = ???

        @relayWorkerExecution
        override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = ???
    }*/
}
