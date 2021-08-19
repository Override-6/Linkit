/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.channel.request.{RequestPacketBundle, RequestPacketChannel, Submitter}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, PacketAttributes, PacketCoordinates}
import fr.linkit.engine.connection.packet.AbstractChannelPacketBundle

case class DefaultRequestChannelPacketBundle(channel: RequestPacketChannel,
                                             override val packet: RequestPacket,
                                             override val coords: PacketCoordinates,
                                             override val responseSubmitter: Submitter[Unit]) extends AbstractChannelPacketBundle(channel) with RequestPacketBundle {

    override val attributes: PacketAttributes = packet.getAttributes

    override def getChannel: RequestPacketChannel = channel
}
