/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, RequestPacketChannel, Submitter, SubmitterPacket}
import fr.linkit.api.gnom.packet.{PacketAttributes, PacketCoordinates}
import fr.linkit.engine.gnom.packet.AbstractPacketChannelBundle

case class DefaultRequestPacketChannelBundle(channel          : RequestPacketChannel,
                                             packet           : SubmitterPacket,
                                             coords           : PacketCoordinates,
                                             responseSubmitter: Submitter[Unit],
                                             ordinal          : Option[Int]) extends AbstractPacketChannelBundle(channel) with RequestPacketBundle {

    def this(channel: RequestPacketChannel, bundle: RequestPacketBundle) {
        this(channel, bundle.packet, bundle.coords, bundle.responseSubmitter, bundle.ordinal)
    }

    override val packetID  : String           = s"@${coords.path.mkString("/")}$$${coords.senderID}:${ordinal.getOrElse("??")}"
    override val attributes: PacketAttributes = packet.getAttributes

    override def getChannel: RequestPacketChannel = channel
}
