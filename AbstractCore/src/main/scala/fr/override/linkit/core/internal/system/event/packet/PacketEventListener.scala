package fr.`override`.linkit.core.internal.system.event.packet

import fr.`override`.linkit.skull.internal.system.event.packet.PacketEvents._

abstract class PacketEventListener extends EventListener {

    def onPacketWritten(event: PacketWrittenEvent): Unit = ()

    def onPacketSent(event: PacketSentEvent): Unit = ()

    def onDedicatedPacketSent(event: DedicatedPacketSentEvent): Unit = ()

    def onBroadcastPacketSent(event: BroadcastPacketSentEvent): Unit = ()

    def onPacketReceived(event: PacketReceivedEvent): Unit = ()

    def onPacketInjected(event: PacketInjectedEvent): Unit = ()

}
