package fr.`override`.linkit.api.packet.traffic.channel

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketInjections}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

import scala.collection.mutable

class PacketChannelCategories(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val categories = mutable.Map.empty[String, PacketInjectable]

    @relayWorkerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach(packet => packet match {
            case WrappedPacket(category, subPacket) =>
                val injection = PacketInjections.unhandled(coordinates, packet)
                categories.get(category).foreach(_.inject(injection))

            case _ => throw new UnexpectedPacketException(s"Received unexpected packet $packet")
        })
    }

    def createCategory[C <: PacketInjectable](name: String, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        if (categories.contains(name))
            throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        val writer = traffic.newWriter(identifier, WrappedPacket(name, _))
        val channel = factory.createNew(scopeFactory(writer))
        categories.put(name, channel)
        channel
    }

}

object PacketChannelCategories extends PacketInjectableFactory[PacketChannelCategories] {
    override def createNew(scope: ChannelScope): PacketChannelCategories = {
        new PacketChannelCategories(scope)
    }
}