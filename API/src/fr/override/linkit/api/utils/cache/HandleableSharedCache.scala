package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

abstract class HandleableSharedCache(identifier: Int, channel: CommunicationPacketChannel) extends SharedCache {

    def handlePacket(packet: Packet, coords: PacketCoordinates)

    def currentContent: Array[Any]

    protected def sendRequest(packet: Packet): Unit = channel.sendRequest(WrappedPacket(s"$identifier", packet))

}
