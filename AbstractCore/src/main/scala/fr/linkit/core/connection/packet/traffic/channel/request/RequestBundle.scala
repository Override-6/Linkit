package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.traffic.PacketChannel
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, PacketAttributes}
import fr.linkit.core.connection.packet.AbstractBundle

case class RequestBundle(channel: PacketChannel,
                         override val packet: RequestPacket,
                         override val coords: DedicatedPacketCoordinates,
                         responseSubmitter: ResponseSubmitter) extends AbstractBundle(channel) {

    override val attributes: PacketAttributes = packet.getAttributes
}
