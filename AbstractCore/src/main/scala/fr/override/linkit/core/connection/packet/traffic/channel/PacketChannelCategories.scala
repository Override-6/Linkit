package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.internal.concurrency.workerExecution
import fr.`override`.linkit.api.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import .PacketInjection
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory}

import scala.collection.mutable

class PacketChannelCategories(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val categories = mutable.Map.empty[String, PacketInjectable]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach {
            case WrappedPacket(category, subPacket) =>
                val injection = PacketInjections.unhandled(coordinates, subPacket)
                categories.get(category).foreach(_.inject(injection))

            case packet => throw new UnexpectedPacketException(s"Received unexpected packet $packet")
        }
    }

    def createCategory[C <: PacketInjectable](name: String, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        //TODO if (categories.contains(name)) <- Has need to be removed due to the client/server reconnection.
        //         throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        categories.getOrElseUpdate(name, {
            val writer = traffic.newWriter(identifier, WrappedPacket(name, _))
            val channel = factory.createNew(scopeFactory(writer))
            categories.put(name, channel)
            channel
        }).asInstanceOf[C]
    }

}

object PacketChannelCategories extends PacketInjectableFactory[PacketChannelCategories] {
    override def createNew(scope: ChannelScope): PacketChannelCategories = {
        new PacketChannelCategories(scope)
    }
}