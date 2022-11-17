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

import fr.linkit.api.gnom.network.tag.{EngineSelector, NetworkFriendlyEngineTag, TagSelection}
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.{AbstractAttributesPresence, SimplePacketAttributes}

object ChannelScopes {

    final case class SelectiveScope private(override val writer: PacketWriter, selection: TagSelection[NetworkFriendlyEngineTag])
            extends AbstractAttributesPresence with ChannelScope {

        private val resolver: EngineSelector = writer.traffic.connection.network

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePackets(packet, attributes, selection)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, target: TagSelection[NetworkFriendlyEngineTag]): Unit = {
            assertAuthorised(target)
            defaultAttributes.drainAttributes(attributes)
            writer.writePackets(packet, attributes, target)
        }

        override def sendToAll(packet: Packet): Unit = {
            sendToAll(packet, SimplePacketAttributes.empty)
        }

        override def sendTo(packet: Packet, tag: TagSelection[NetworkFriendlyEngineTag]): Unit = {
            sendTo(packet, SimplePacketAttributes.empty, tag)
        }


        override def areAuthorised(tag: TagSelection[NetworkFriendlyEngineTag]): Boolean = resolver.isIncluded(tag, this.selection)

        override def equals(obj: Any): Boolean = {
            obj match {
                case s: SelectiveScope => s.resolver == resolver && s.resolver.isEquivalent(s.selection, selection)
                case _                 => false
            }
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

    }


    def apply(selection: TagSelection[NetworkFriendlyEngineTag]): ScopeFactory[ChannelScope] = {
        SelectiveScope(_, selection)
    }

}
