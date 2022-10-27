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

package fr.linkit.api.gnom.packet.channel

import fr.linkit.api.gnom.network.EngineTag
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketAttributesPresence}
import fr.linkit.api.internal.system.ForbiddenIdentifierException

trait ChannelScope extends PacketAttributesPresence {

    val writer: PacketWriter

    val traffic: PacketTraffic = writer.traffic

    def sendToAll(packet: Packet, attributes: PacketAttributes): Unit

    def sendToAll(packet: Packet): Unit

    def sendTo(packet: Packet, attributes: PacketAttributes, tags: Array[EngineTag]): Unit

    def sendTo(packet: Packet, tags: Array[EngineTag]): Unit

    def areAuthorised(tags: Array[EngineTag]): Boolean


    def assertAuthorised(identifiers: Array[EngineTag]): Unit = {
        if (!areAuthorised(identifiers))
            throw new ForbiddenIdentifierException(s"one of the identifier in '${identifiers.mkString("Array(", ", ", ")")}' is not authorised by this scope.")
    }

    def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S

    def equals(obj: Any): Boolean

}

object ChannelScope {

    trait ScopeFactory[S <: ChannelScope] {

        def apply(writer: PacketWriter): S
    }

}