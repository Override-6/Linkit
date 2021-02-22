package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.system.event.EventListener
import fr.`override`.linkit.api.system.event.packet.PacketEvents._

abstract class PacketEventListener extends EventListener {

    def onPacketWritten(event: PacketWrittenEvent): Unit= ()

    def onPacketSent(event: PacketSentEvent): Unit= ()

    def onDedicatedPacketSent(event: DedicatedPacketSentEvent): Unit= ()

    def onBroadcastPacketSent(event: BroadcastPacketSentEvent): Unit= ()

    def onPacketReceived(event: PacketReceivedEvent): Unit= ()

    def onPacketInjected(event: PacketInjectedEvent): Unit= ()

}
