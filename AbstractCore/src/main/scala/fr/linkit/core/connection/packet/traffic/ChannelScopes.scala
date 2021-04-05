package fr.linkit.core.connection.packet.traffic

import fr.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketWriter}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketAttributesPresence}
import fr.linkit.core.connection.packet.{AbstractAttributePresence, SimplePacketAttributes}

object ChannelScopes {

    @volatile private var scopeNumber = 0

    private def nextID: Int = {
        scopeNumber += 1
        scopeNumber
    }

    final case class BroadcastScope private(override val writer: PacketWriter)
            extends AbstractAttributePresence with ChannelScope {

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writeBroadcastPacket(packet, attributes)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: String*): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, targetIDs: _*)
        }

        override def sendToAll(packet: Packet): Unit = sendToAll(packet, SimplePacketAttributes.empty)

        override def sendTo(packet: Packet, targetIDs: String*): Unit = sendTo(packet, SimplePacketAttributes.empty, targetIDs: _*)

        override def areAuthorised(identifiers: String*): Boolean = true //everyone is authorised in a BroadcastScope

        override def canConflictWith(scope: ChannelScope): Boolean = {
            //As Long As everyone is authorised by a BroadcastScope,
            //the other scope wouldn't conflict with this scope only if it discards all identifiers.
            scope.isInstanceOf[BroadcastScope] || scope.canConflictWith(this)
        }

        override def equals(obj: Any): Boolean = {
            obj.isInstanceOf[BroadcastScope]
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

        override def getID: Int = writer.identifier
    }

    final case class ReservedScope private(override val writer: PacketWriter, authorisedIds: String*)
            extends AbstractAttributePresence with ChannelScope {

        override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, authorisedIds: _*)
        }

        override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: String*): Unit = {
            assertAuthorised(targetIDs: _*)
            defaultAttributes.drainAttributes(attributes)
            writer.writePacket(packet, attributes, targetIDs: _*)
        }

        override def sendToAll(packet: Packet): Unit = {
            sendToAll(packet, SimplePacketAttributes.empty)
        }

        override def sendTo(packet: Packet, targetIDs: String*): Unit = {
            sendTo(packet, SimplePacketAttributes.empty, targetIDs: _*)
        }

        override def areAuthorised(identifier: String*): Boolean = {
            authorisedIds.containsSlice(identifier)
        }

        override def canConflictWith(scope: ChannelScope): Boolean = {
            scope.areAuthorised(authorisedIds: _*)
        }

        override def equals(obj: Any): Boolean = {
            obj match {
                case s: ReservedScope => s.authorisedIds == this.authorisedIds
                case _                => false
            }
        }

        override def shareWriter[S <: ChannelScope](factory: ScopeFactory[S]): S = factory(writer)

        override def getID: Int = writer.identifier
    }

    def broadcast: ScopeFactory[BroadcastScope] = BroadcastScope(_)

    def reserved(authorised: String*): ScopeFactory[ReservedScope] = ReservedScope(_, authorised: _*)

}
