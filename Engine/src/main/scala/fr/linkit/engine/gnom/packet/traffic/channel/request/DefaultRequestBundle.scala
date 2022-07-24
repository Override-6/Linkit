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

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, RequestPacketChannel, Submitter, SubmitterPacket}
import fr.linkit.api.gnom.packet.{PacketAttributes, PacketCoordinates}
import fr.linkit.engine.gnom.packet.AbstractChannelPacketBundle

case class DefaultRequestBundle(channel: RequestPacketChannel,
                                override val packet: SubmitterPacket,
                                override val coords: PacketCoordinates,
                                override val responseSubmitter: Submitter[Unit]) extends AbstractChannelPacketBundle(channel) with RequestPacketBundle {

    override val attributes: PacketAttributes = packet.getAttributes

    override def getChannel: RequestPacketChannel = channel
}
