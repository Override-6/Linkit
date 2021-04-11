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

package fr.linkit.api.connection.packet.traffic

import fr.linkit.api.connection.packet.channel.PacketChannel
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}

trait PacketSender extends PacketChannel {

    def send(packet: Packet, attributes: PacketAttributes): Unit

    @throws[IllegalArgumentException]("If targets contains an identifier that is not authorised by his scope.")
    def sendTo(packet: Packet, attributes: PacketAttributes, targets: String*): Unit

    def send(packet: Packet): Unit

    @throws[IllegalArgumentException]("If targets contains an identifier that is not authorised by his scope.")
    def sendTo(packet: Packet, targets: String*): Unit

}
