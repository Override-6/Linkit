/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketWriter
import fr.linkit.core.connection.packet.traffic.{AbstractPacketTraffic, WriterInfo}
import fr.linkit.server.connection.ServerConnection

class ServerPacketTraffic(serverConnection: ServerConnection) extends AbstractPacketTraffic(serverConnection.supportIdentifier) {

    override val supportIdentifier: String = serverConnection.supportIdentifier
    override val serverIdentifier : String = supportIdentifier

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new ServerPacketWriter(serverConnection, WriterInfo(this, identifier, transform))
    }
}
