package fr.`override`.linkit.skull.connection.packet.traffic

import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.internal.system.ForbiddenIdentifierException

trait ChannelScope {
    val writer: PacketWriter

    def sendToAll(packet: Packet): Unit

    def sendTo(packet: Packet, targetIDs: String*): Unit

    def areAuthorised(identifiers: String*): Boolean

    def copyFromWriter(writer: PacketWriter): ChannelScope

    def canConflictWith(scope: ChannelScope): Boolean

    def assertAuthorised(identifiers: String*): Unit = {
        if (!areAuthorised(identifiers: _*))
            throw new ForbiddenIdentifierException(s"this identifier '${identifiers}' is not authorised by this scope.")
    }

    def equals(obj: Any): Boolean

}

object ChannelScope {

    final case class BroadcastScope private(override val writer: PacketWriter) extends ChannelScope {
        override def sendToAll(packet: Packet): Unit = writer.writeBroadcastPacket(packet)

        override def sendTo(packet: Packet, targetIDs: String*): Unit = {
            writer.writePacket(packet, targetIDs: _*)
        }

        override def areAuthorised(identifiers: String*): Boolean = true //everyone is authorised in a BroadcastScope

        override def canConflictWith(scope: ChannelScope): Boolean = {
            //As Long As everyone is authorised by a BroadcastScope,
            //the other scope wouldn't conflict with this scope only if it discards all identifiers.
            scope.isInstanceOf[BroadcastScope] || scope.canConflictWith(this)
        }

        override def copyFromWriter(writer: PacketWriter): BroadcastScope = BroadcastScope(writer)

        override def equals(obj: Any): Boolean = {
            obj.isInstanceOf[BroadcastScope]
        }
    }

    final case class ReservedScope private(override val writer: PacketWriter, authorisedIds: String*) extends ChannelScope {
        override def sendToAll(packet: Packet): Unit = {
            authorisedIds.foreach(writer.writePacket(packet, _))
        }

        override def sendTo(packet: Packet, targetIDs: String*): Unit = {
            assertAuthorised(targetIDs: _*)
            writer.writePacket(packet, targetIDs: _*)
        }

        override def areAuthorised(identifier: String*): Boolean = {
            authorisedIds.containsSlice(identifier)
        }

        override def copyFromWriter(writer: PacketWriter): ReservedScope = ReservedScope(writer, authorisedIds: _*)

        override def canConflictWith(scope: ChannelScope): Boolean = {
            scope.areAuthorised(authorisedIds: _*)
        }

        override def equals(obj: Any): Boolean = {
            obj match {
                case s: ReservedScope => s.authorisedIds == this.authorisedIds
                case _ => false
            }
        }
    }

    trait ScopeFactory[S <: ChannelScope] {
        def apply(writer: PacketWriter): S
    }

    def broadcast: ScopeFactory[BroadcastScope] = BroadcastScope(_)

    def reserved(authorised: String*): ScopeFactory[ReservedScope] = ReservedScope(_, authorised: _*)

}