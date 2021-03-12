package fr.`override`.linkit.api.system.evente.packet

import fr.`override`.linkit.api.system.evente.EventListener
import fr.`override`.linkit.api.system.evente.packet.PacketEvents._

abstract class PacketEventListener extends EventListener {

    def onPacketWritten(event: PacketWrittenEvent): Unit = ()

    def onPacketSent(event: PacketSentEvent): Unit = ()

    def onDedicatedPacketSent(event: DedicatedPacketSentEvent): Unit = ()

    def onBroadcastPacketSent(event: BroadcastPacketSentEvent): Unit = ()

    def onPacketReceived(event: PacketReceivedEvent): Unit = ()

    def onPacketInjected(event: PacketInjectedEvent): Unit = ()

}
