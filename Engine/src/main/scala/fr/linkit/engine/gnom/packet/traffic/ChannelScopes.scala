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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.network.{EngineTag, Everyone}
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.{AbstractAttributesPresence, SimplePacketAttributes}

object ChannelScopes {

    final case class SelectiveScope private(override val writer: PacketWriter, excludeBaseTags: Boolean, baseTags: Array[EngineTag])
            extends AbstractAttributesPresence with ChannelScope {

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePackets(packet, attributes, baseTags, excludeBaseTags)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, targetTags: Array[EngineTag]): Unit = {
            assertAuthorised(targetTags)
            defaultAttributes.drainAttributes(attributes)
            writer.writePackets(packet, attributes, targetTags, excludeBaseTags)
        }

        override def sendToAll(packet: Packet): Unit = {
            sendToAll(packet, SimplePacketAttributes.empty)
        }

        override def sendTo(packet: Packet, targetTags: Array[EngineTag]): Unit = {
            sendTo(packet, SimplePacketAttributes.empty, targetTags)
        }

        override def areAuthorised(tags: Array[EngineTag]): Boolean = excludeBaseTags != baseTags.containsSlice(tags)

        override def equals(obj: Any): Boolean = {
            obj match {
                case s: SelectiveScope => s.baseTags sameElements this.baseTags
                case _                 => false
            }
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

    }

    def discardCurrent: ScopeFactory[ChannelScope] = writer => SelectiveScope(writer, true, Array(writer.currentEngineName))

    def discards(discarded: EngineTag*): ScopeFactory[ChannelScope] = SelectiveScope(_, true, Array(discarded: _*))

    def broadcast: ScopeFactory[ChannelScope] = include(Everyone)

    def include(authorised: EngineTag*): ScopeFactory[ChannelScope] = {
        SelectiveScope(_, false, Array(authorised: _*))
    }

}
