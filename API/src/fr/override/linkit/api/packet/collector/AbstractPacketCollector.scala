package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketWriter}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable

abstract class AbstractPacketCollector(writer: PacketWriter, collectorID: Int, handleOnSubInjected: Boolean) extends PacketCollector {
    override val ownerID: String = writer.ownerID
    override val identifier: Int = collectorID
    private val subChannels: mutable.Map[String, PacketInjectable] = mutable.Map.empty
    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed

    override def sendPacket(packet: Packet, targetID: String): Unit = {
        writer.writePacket(packet, identifier, targetID)
    }

    override def broadcastPacket(packet: Packet): Unit = {
        writer.writePacket(packet, identifier, "BROADCAST")
    }

    override def subChannel[C <: PacketChannel](targetID: String, factory: PacketChannelFactory[C]): C = {
        val fragOpt = subChannels.get(targetID)

        if (fragOpt.isDefined && fragOpt.get.isOpen) {
            fragOpt.get match {
                case subChannel: C => return subChannel
                case _ => throw new IllegalArgumentException("A sub channel dependent of this collector is already bound to this target")
            }
        }

        val channel = factory.createNew(writer, identifier, targetID)
        subChannels.put(targetID, channel)
        channel
    }


    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val opt = subChannels.get(coordinates.senderID)
        val injectSub = opt.isDefined
        if (injectSub) {
            opt.get.injectPacket(packet, coordinates)
            return
        }
        if (!injectSub || handleOnSubInjected)
            handlePacket(packet, coordinates)
    }

    protected def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit


}
