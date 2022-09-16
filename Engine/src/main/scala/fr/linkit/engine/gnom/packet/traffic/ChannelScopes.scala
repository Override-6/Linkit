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

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.spi.gnom.packet.AbstractAttributesPresence

object ChannelScopes {

    final case class BroadcastScope private(override val writer: PacketWriter, discarded: Array[String])
            extends AbstractAttributesPresence with ChannelScope {

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writeBroadcastPacket(packet, attributes, discarded)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, targetIDs)
        }

        override def sendToAll(packet: Packet): Unit = sendToAll(packet, SimplePacketAttributes.empty)

        override def sendTo(packet: Packet, targetIDs: Array[String]): Unit = sendTo(packet, SimplePacketAttributes.empty, targetIDs)

        override def areAuthorised(identifiers: Array[String]): Boolean = true //everyone is authorised in a BroadcastScope

        override def canConflictWith(scope: ChannelScope): Boolean = {
            //As Long As everyone is authorised by a BroadcastScope,
            //the other scope wouldn't conflict with this scope only if it discards all identifiers.
            scope.isInstanceOf[BroadcastScope] || scope.canConflictWith(this)
        }

        override def equals(obj: Any): Boolean = {
            obj.isInstanceOf[BroadcastScope]
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

    }

    final case class RetainerScope private(override val writer: PacketWriter, authorisedIds: Array[String])
            extends AbstractAttributesPresence with ChannelScope {

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, authorisedIds)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit = {
            assertAuthorised(targetIDs)
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, targetIDs)
        }

        override def sendToAll(packet: Packet): Unit = {
            sendToAll(packet, SimplePacketAttributes.empty)
        }

        override def sendTo(packet: Packet, targetIDs: Array[String]): Unit = {
            sendTo(packet, SimplePacketAttributes.empty, targetIDs)
        }

        override def areAuthorised(identifier: Array[String]): Boolean = {
            authorisedIds.containsSlice(identifier)
        }

        override def canConflictWith(scope: ChannelScope): Boolean = {
            scope.areAuthorised(authorisedIds)
        }

        override def equals(obj: Any): Boolean = {
            obj match {
                case s: RetainerScope => s.authorisedIds sameElements this.authorisedIds
                case _                => false
            }
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

    }

    def broadcast: ScopeFactory[BroadcastScope] = BroadcastScope(_, Array.empty)

    def discardCurrent: ScopeFactory[BroadcastScope] = writer => BroadcastScope(writer, Array(writer.currentIdentifier))

    def discards(discarded: String*): ScopeFactory[BroadcastScope] = BroadcastScope(_, Array(discarded: _*))

    def include(authorised: String*): ScopeFactory[RetainerScope] = {
        RetainerScope(_, Array(authorised: _*))
    }

}
