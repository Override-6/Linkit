/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.system.event.packet

import fr.`override`.linkit.api.connection.packet.serialization.{PacketSerializationResult, Serializer}
import fr.`override`.linkit.api.connection.packet.traffic.PacketInjectable
import fr.`override`.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet, PacketCoordinates}

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
                                  serializer: Class[_ <: Serializer],
                                  bytes: Array[Byte]) extends PacketEvent {

        override def getHooks(category: PacketEventHooks): Array[PacketEventHook] = !!(Array(category.packetWritten))
    }

    case class PacketReceivedEvent(override val packet: Packet,
                                   coordinates: PacketCoordinates,
                                   serializer: Class[_ <: Serializer],
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
        PacketWrittenEvent(result.packet, result.coords, result.serializer().getClass, result.bytes)
    }

    def packetReceived(packet: Packet, coords: PacketCoordinates, serializer: Serializer, bytes: Array[Byte]): PacketReceivedEvent = {
        PacketReceivedEvent(packet, coords, serializer.getClass, bytes)
    }

    def packetInjected(packet: Packet, coords: PacketCoordinates, injectable: PacketInjectable): PacketInjectedEvent = {
        PacketInjectedEvent(packet, coords, injectable)
    }

    private def !![A](any: Any): A = any.asInstanceOf[A]

}
