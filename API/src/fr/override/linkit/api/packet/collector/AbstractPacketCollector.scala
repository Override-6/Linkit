package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketSender}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable

abstract class AbstractPacketCollector(sender: PacketSender, collectorID: Int) extends PacketCollector {
    override val ownerID: String = sender.ownerID
    override val identifier: Int = collectorID
    private val fragments: mutable.Map[String, PacketInjectable] = mutable.Map.empty
    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed

    override def sendPacket(packet: Packet, targetID: String): Unit = {
        sender.sendPacket(packet, identifier, targetID)
    }

    override def subChannel[C <: PacketChannel](targetID: String, factory: PacketChannelFactory[C]): C = {
        val fragOpt = fragments.get(targetID)

        if (fragOpt.isDefined && fragOpt.get.isOpen) {
            fragOpt.get match {
                case subChannel: C => return subChannel
                case _ => throw new IllegalArgumentException("A sub channel dependent of this collector is already bound to this target")
            }
        }

        val channel = factory.createNew(sender, identifier, targetID)
        fragments.put(targetID, channel)
        channel
    }


    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val opt = fragments.get(coordinates.senderID)
        if (opt.isDefined)
            opt.get.injectPacket(packet, coordinates)
        handlePacket(packet, coordinates)
    }

    protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit


}
