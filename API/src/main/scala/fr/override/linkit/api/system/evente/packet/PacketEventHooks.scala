package fr.`override`.linkit.api.system.evente.packet

import fr.`override`.linkit.api.system.evente.packet.PacketEvents._
import fr.`override`.linkit.api.system.evente.{EventHookCategory, SimpleEventHook}

//noinspection TypeAnnotation
class PacketEventHooks extends EventHookCategory {
    type L = PacketEventListener
    val packetWritten = SimpleEventHook[L, PacketWrittenEvent](_.onPacketWritten(_))

    val packetSent: Unit = SimpleEventHook[L, PacketSentEvent](_.onPacketSent(_))

    val dedicatedPacketSent = SimpleEventHook[L, DedicatedPacketSentEvent](_.onDedicatedPacketSent(_))

    val broadcastPacketSent = SimpleEventHook[L, BroadcastPacketSentEvent](_.onBroadcastPacketSent(_))

    val packetReceived = SimpleEventHook[L, PacketReceivedEvent](_.onPacketReceived(_))

    val packetInjected = SimpleEventHook[L, PacketInjectedEvent](_.onPacketInjected(_))
}
