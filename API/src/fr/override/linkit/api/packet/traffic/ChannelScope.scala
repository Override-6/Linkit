package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.exception.ForbiddenIdentifierException
import fr.`override`.linkit.api.packet.Packet

import scala.collection.mutable.{Set => MSet}

trait ChannelScope {
    val writer: PacketWriter

    def sendToAll(packet: Packet): Unit

    def sendTo(targetID: String, packet: Packet): Unit

    def isAuthorised(identifier: String): Boolean

    def copyFromWriter(writer: PacketWriter): ChannelScope

    def canConflictWith(scope: ChannelScope): Boolean

    def assertAuthorised(identifier: String): Unit = {
        if (!isAuthorised(identifier))
            throw new ForbiddenIdentifierException(s"this identifier '${identifier}' is not authorised by this scope.")
    }

}

object ChannelScope {

    final case class BroadcastScope private(override val writer: PacketWriter) extends ChannelScope {
        override def sendToAll(packet: Packet): Unit = writer.writeBroadcastPacket(packet)

        override def sendTo(targetID: String, packet: Packet): Unit = {
            writer.writePacket(packet, targetID)
        }

        override def isAuthorised(identifier: String): Boolean = true //everyone is authorised in a BroadcastScope

        override def canConflictWith(scope: ChannelScope): Boolean = {
            //As Long As everyone is authorised by a BroadcastScope,
            //the other scope wouldn't conflict with this scope only if it discards all identifiers.
            scope.isInstanceOf[BroadcastScope] || scope.canConflictWith(this)
        }

        override def copyFromWriter(writer: PacketWriter): BroadcastScope = BroadcastScope(writer)
    }

    final case class ImmutableScope private(override val writer: PacketWriter, authorisedIds: String*) extends ChannelScope {
        override def sendToAll(packet: Packet): Unit = {
            authorisedIds.foreach(writer.writePacket(packet, _))
        }

        override def sendTo(targetID: String, packet: Packet): Unit = {
            assertAuthorised(targetID)
            writer.writePacket(packet, targetID)
        }

        override def isAuthorised(identifier: String): Boolean = {
            println("Checking authorisation in immutable...")
            println(s"tested identifier = ${identifier}")
            println(s"authorisedIds = ${authorisedIds}")

            authorisedIds.contains(identifier)
        }

        override def copyFromWriter(writer: PacketWriter): ImmutableScope = ImmutableScope(writer, authorisedIds: _*)

        override def canConflictWith(scope: ChannelScope): Boolean = {
            authorisedIds.exists(scope.isAuthorised)
        }
    }

    final case class MutableScope private(override val writer: PacketWriter) extends ChannelScope {
        private val authorisedIds = MSet.empty[String]
        @volatile private var reverseAuthorisation = false

        override def sendToAll(packet: Packet): Unit = {
            if (reverseAuthorisation) {
                //NOTE: This array represent the DISCARDED identifiers that will NOT receive any packet
                val discarded = authorisedIds.toArray
                writer.writeBroadcastPacket(packet, discarded)
            } else {
                authorisedIds.foreach(writer.writePacket(packet, _))
            }
        }

        override def sendTo(targetID: String, packet: Packet): Unit = {
            assertAuthorised(targetID)
            writer.writePacket(packet, targetID)
        }

        override def isAuthorised(identifier: String): Boolean = {
            println("Checking authorisation in mutable...")
            println(s"tested identifier = ${identifier}")
            println(s"authorisedIds = ${authorisedIds}")
            println(s"reverseAuthorisation = ${reverseAuthorisation}")

            val authorised = authorisedIds.contains(identifier)
            (reverseAuthorisation && !authorised) || (!reverseAuthorisation && authorised)
        }

        override def copyFromWriter(writer: PacketWriter): MutableScope = {
            val scope = MutableScope(writer)
            scope.authorisedIds.addAll(authorisedIds)
            scope.reverseAuthorisation = reverseAuthorisation
            scope
        }

        def authorise(identifier: String): Unit = authorisedIds += identifier

        def prohibit(identifier: String): Unit = authorisedIds -= identifier

        def authoriseAll(): Unit = {
            reverseAuthorisation = true
            authorisedIds.clear()
        }

        def prohibitAll(): Unit = {
            reverseAuthorisation = false
            authorisedIds.clear()
        }

        override def canConflictWith(scope: ChannelScope): Boolean = {
            authorisedIds.exists(scope.isAuthorised)
        }
    }

    trait ScopeFactory[S <: ChannelScope] {
        def apply(writer: PacketWriter): S
    }

    def broadcast: ScopeFactory[BroadcastScope] = BroadcastScope(_)

    def immutable(default: String*): ScopeFactory[ImmutableScope] = ImmutableScope(_, default: _*)

    def mutable(default: String*): ScopeFactory[MutableScope] = writer => {
        val scope = MutableScope(writer)
        default.foreach(scope.authorise)
        scope
    }

}
