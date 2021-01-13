package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable

abstract class AbstractPacketCollector(traffic: PacketTraffic, collectorID: Int, defaultSubChannelTransparent: Boolean)
        extends PacketCollector {


    override val ownerID: String = traffic.ownerID
    override val identifier: Int = collectorID
    override val injector: PacketTraffic = traffic

    private val subChannels: mutable.Map[String, SubInjectableContainer] = mutable.Map.empty
    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed

    override def sendPacket(packet: Packet, targetID: String): Unit = {
        injector.writePacket(packet, identifier, targetID)
    }

    override def broadcastPacket(packet: Packet): Unit = {
        injector.writePacket(packet, identifier, "BROADCAST")
    }

    /**
     * Creates a PacketChannel that will be handled by this packet collector. <br>
     * The packet channel can send and receive packets from the target.  <br>
     * In other words, this will create a sub channel that is bound to a specific relay.  <br>
     * If the sub channel receives a packet, the parent collector will handle the packet as well if the
     * children authorises it
     *
     * @param targetID the bounded relay identifier that the sub channel will focus
     * @param transparent if ture, the children is set as 'transparent',
     *                    that means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * @param factory the factory that wil determine the kind of PacketChannel that will be used.
     * */
    override def subChannel[C <: PacketChannel](targetID: String,
                                                factory: PacketChannelFactory[C],
                                                transparent: Boolean = defaultSubChannelTransparent): C = {
        val fragOpt = subChannels.get(targetID)
        if (fragOpt.exists(_.subInjectable.isOpen)) {
            fragOpt.get.subInjectable match {
                case subChannel: C => return subChannel
                case _ => throw new IllegalArgumentException("A sub channel dependent of this collector is already bound to this target")
            }
        }

        val channel = factory.createNew(injector, identifier, targetID)
        subChannels.put(targetID, SubInjectableContainer(channel, transparent))
        channel
    }


    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val opt = subChannels.get(coordinates.senderID)
        val subChannelInject = opt.isDefined
        lazy val subChannelContainer = opt.get

        if (subChannelInject) {
            val subChannel = subChannelContainer.subInjectable
            subChannel.injectPacket(packet, coordinates)
        }
        if (!subChannelInject || subChannelContainer.transparent)
            handlePacket(packet, coordinates)
    }

    protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit

    private case class SubInjectableContainer(subInjectable: PacketInjectable, transparent: Boolean)

}
