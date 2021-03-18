package fr.`override`.linkit.core.local.system.event.packet

import fr.`override`.linkit.api.connection.packet.serialization.ObjectSerializer
import fr.`override`.linkit.api.connection.packet.traffic.PacketInjectable
import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

object PacketEvents {

    sealed trait PacketSentEvent extends PacketEvent {
        val coordinates: PacketCoordinates
    }

    case class DedicatedPacketSentEvent(override val packet: Packet,
                                        override val coordinates: DedicatedPacketCoordinates)
            extends PacketSentEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = {
            !!(Array(category.dedicatedPacketSent))
        }
    }

    case class BroadcastPacketSentEvent(override val packet: Packet,
                                        override val coordinates: BroadcastPacketCoordinates)
            extends PacketSentEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = {
            !!(Array(category.broadcastPacketSent))
        }
    }

    case class PacketWrittenEvent(override val packet: Packet,
                                  coordinates: PacketCoordinates,
                                  serializer: Class[_ <: ObjectSerializer],
                                  bytes: Array[Byte]) extends PacketEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = !!(Array(category.packetWritten))
    }

    case class PacketReceivedEvent(override val packet: Packet,
                                   coordinates: PacketCoordinates,
                                   serializer: Class[_ <: ObjectSerializer],
                                   bytes: Array[Byte]) extends PacketEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = !!(Array(category.packetReceived))
    }

    case class PacketInjectedEvent(override val packet: Packet,
                                   coordinates: PacketCoordinates,
                                   injectable: PacketInjectable) extends PacketEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = !!(Array(category.packetInjected))
    }

    def packedSent(packet: Packet, coordinates: PacketCoordinates): PacketSentEvent = {
        coordinates match {
            case dedicated: DedicatedPacketCoordinates => DedicatedPacketSentEvent(packet, dedicated)
            case broadcast: BroadcastPacketCoordinates => BroadcastPacketSentEvent(packet, broadcast)
        }
    }

    def packetWritten(result: PacketSerializationResult): PacketWrittenEvent = {
        PacketWrittenEvent(result.packet, result.coords, result.serializer.getClass, result.bytes)
    }

    def packetReceived(packet: Packet, serializer: ObjectSerializer, bytes: Array[Byte]): PacketReceivedEvent = {
        PacketReceivedEvent(packet, serializer.getClass, bytes)
    }

    def packetInjected(packet: Packet, injectable: PacketInjectable): PacketInjectedEvent = {
        PacketInjectedEvent(packet, injectable)
    }

    private def !![A](any: Any): A = any.asInstanceOf[A]

}
