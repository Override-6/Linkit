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

package fr.linkit.api.connection.packet.channel

import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketAttributesPresence}
import fr.linkit.api.local.system.ForbiddenIdentifierException

trait ChannelScope extends PacketAttributesPresence {

    val writer: PacketWriter

    val traffic: PacketTraffic = writer.traffic

    def sendToAll(packet: Packet, attributes: PacketAttributes): Unit

    def sendToAll(packet: Packet): Unit

    def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: String*): Unit

    def sendTo(packet: Packet, targetIDs: String*): Unit

    def areAuthorised(identifiers: String*): Boolean

    def canConflictWith(scope: ChannelScope): Boolean

    def assertAuthorised(identifiers: String*): Unit = {
        if (!areAuthorised(identifiers: _*))
            throw new ForbiddenIdentifierException(s"this identifier '${identifiers}' is not authorised by this scope.")
    }

    def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S

    def equals(obj: Any): Boolean

}

object ChannelScope {

    trait ScopeFactory[S <: ChannelScope] {

        def apply(writer: PacketWriter): S
    }

}