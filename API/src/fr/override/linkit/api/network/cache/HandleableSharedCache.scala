package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

abstract class HandleableSharedCache(override val family: String,
                                     identifier: Int,
                                     channel: CommunicationPacketChannel) extends SharedCache with JustifiedCloseable {

    override def close(reason: CloseReason): Unit = channel.close(reason)

    override def isClosed: Boolean = channel.isClosed

    def handlePacket(packet: Packet, coords: PacketCoordinates)

    def currentContent: Array[Any]

    protected def sendRequest(packet: Packet): Unit = channel.sendRequest(WrappedPacket(s"$family", WrappedPacket(identifier.toString, packet)))

}
