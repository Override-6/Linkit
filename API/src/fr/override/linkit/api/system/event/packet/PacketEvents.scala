package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.packet.serialization.ObjectSerializer
import fr.`override`.linkit.api.packet.traffic.PacketInjectable
import fr.`override`.linkit.api.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.event.packet.PacketEventHooks._

object PacketEvents {

    abstract sealed class PacketSentEvent extends PacketEvent {
        val coordinates: PacketCoordinates
    }

    case class DedicatedPacketSentEvent(override val packet: Packet, override val coordinates: DedicatedPacketCoordinates)
        extends PacketSentEvent {

        override def getHooks: Array[PacketEventHook] = {
            Array(DedicatedPacketSent)
        }
    }


    case class BroadcastPacketSentEvent(override val packet: Packet, override val coordinates: BroadcastPacketCoordinates)
        extends PacketSentEvent {

        override def getHooks: Array[PacketEventHook] = {
            Array(BroadcastPacketSent)
        }
    }

    case class PacketWrittenEvent(override val packet: Packet,
                                  serializer: Class[_ <: ObjectSerializer],
                                  bytes: Array[Byte]) extends PacketEvent {

        override def getHooks: Array[PacketEventHook] = Array(PacketWritten)
    }

    case class PacketReceivedEvent(override val packet: Packet,
                                   serializer: Class[_ <: ObjectSerializer],
                                   bytes: Array[Byte]) extends PacketEvent {

        override def getHooks: Array[PacketEventHook] = Array(PacketReceived)
    }

    case class PacketInjectedEvent(override val packet: Packet,
                                   injectable: PacketInjectable) extends PacketEvent {

        override def getHooks: Array[PacketEventHook] = Array(PacketInjected)
    }

    def packedSent(packet: Packet, coordinates: PacketCoordinates): PacketSentEvent = {
        coordinates match {
            case dedicated: DedicatedPacketCoordinates => DedicatedPacketSentEvent(packet, dedicated)
            case broadcast: BroadcastPacketCoordinates => BroadcastPacketSentEvent(packet, broadcast)
        }
    }

    def packetWritten(packet: Packet, serializer: ObjectSerializer, bytes: Array[Byte]): PacketWrittenEvent = {
        PacketWrittenEvent(packet, serializer.getClass, bytes)
    }

    def packetReceived(packet: Packet, serializer: ObjectSerializer, bytes: Array[Byte]): PacketReceivedEvent = {
        PacketReceivedEvent(packet, serializer.getClass, bytes)
    }

    def packetInjected(packet: Packet, injectable: PacketInjectable): PacketInjectedEvent = {
        PacketInjectedEvent(packet, injectable)
    }


}
