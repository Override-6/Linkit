package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet.channel.PacketChannel
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketBundle, PacketCoordinates}
import fr.linkit.engine.gnom.packet.AbstractChannelPacketBundle

case class DefaultChannelPacketBundle(channel: PacketChannel,
                                      override val packet: Packet,
                                      override val attributes: PacketAttributes,
                                      override val coords: PacketCoordinates) extends AbstractChannelPacketBundle(channel) {

    def this(channel: PacketChannel, bundle: PacketBundle) = {
        this(channel, bundle.packet, bundle.attributes, bundle.coords)
    }

}
object DefaultChannelPacketBundle {
    def apply(channel: PacketChannel, bundle: PacketBundle): DefaultChannelPacketBundle = new DefaultChannelPacketBundle(channel, bundle)
}