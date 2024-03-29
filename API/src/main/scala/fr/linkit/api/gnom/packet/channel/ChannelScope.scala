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

import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, TagSelection}
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketAttributesPresence}
import fr.linkit.api.internal.system.ForbiddenSelectionException

trait ChannelScope extends PacketAttributesPresence {

    val writer: PacketWriter


    def sendToAll(packet: Packet, attributes: PacketAttributes): Unit

    def sendToAll(packet: Packet): Unit

    def sendTo(packet: Packet, attributes: PacketAttributes, tag: TagSelection[NetworkFriendlyEngineTag]): Unit

    def sendTo(packet: Packet, tag: TagSelection[NetworkFriendlyEngineTag]): Unit

    def areAuthorised(tags: TagSelection[NetworkFriendlyEngineTag]): Boolean


    def assertAuthorised(tag: TagSelection[NetworkFriendlyEngineTag]): Unit = {
        if (!areAuthorised(tag))
            throw new ForbiddenSelectionException(s"selection $tag is not authorized")
    }

    def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S

    def equals(obj: Any): Boolean

}

object ChannelScope {

    trait ScopeFactory[S <: ChannelScope] {

        def apply(writer: PacketWriter): S
    }

}