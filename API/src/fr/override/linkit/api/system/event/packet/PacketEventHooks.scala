package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.system.event.SimpleEventHook
import fr.`override`.linkit.api.system.event.packet.PacketEvents._

//noinspection TypeAnnotation
object PacketEventHooks {
    type L = PacketEventListener
    val PacketWritten = SimpleEventHook[L, PacketWrittenEvent](_.onPacketWritten(_))

    val PacketSent: Unit = SimpleEventHook[L, PacketSentEvent](_.onPacketSent(_))

    val DedicatedPacketSent = SimpleEventHook[L, DedicatedPacketSentEvent](_.onDedicatedPacketSent(_))

    val BroadcastPacketSent = SimpleEventHook[L, BroadcastPacketSentEvent](_.onBroadcastPacketSent(_))

    val PacketReceived = SimpleEventHook[L, PacketReceivedEvent](_.onPacketReceived(_))

    val PacketInjected = SimpleEventHook[L, PacketInjectedEvent](_.onPacketInjected(_))
}
