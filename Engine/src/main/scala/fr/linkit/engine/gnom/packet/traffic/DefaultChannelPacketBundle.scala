/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

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