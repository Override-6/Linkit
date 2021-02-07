package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketWriter}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable

class PacketChannelCategories(writer: PacketWriter,
                              connectedID: String) extends AbstractPacketChannel(writer, connectedID) {

    private val categories = mutable.Map.empty[String, PacketInjectable]

    @relayWorkerExecution
    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        packet match {
            case WrappedPacket(category, subPacket) =>
                categories.get(category).foreach(_.injectPacket(subPacket, coordinates))

            case _ => throw new UnexpectedPacketException(s"Received unexpected packet $packet")
        }
    }

    def createCategory[C <: PacketChannel](name: String, factory: PacketChannelFactory[C]): C = {
        if (categories.contains(name))
            throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        val writer = traffic.newWriter(identifier, WrappedPacket(name, _))
        val channel = factory.createNew(writer, connectedID)
        categories.put(name, channel)
        channel
    }

}
