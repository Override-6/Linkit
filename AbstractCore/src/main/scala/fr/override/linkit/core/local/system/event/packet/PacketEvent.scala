package fr.`override`.linkit.core.local.system.event.packet

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.local.system.event.{Event, EventHook}

trait PacketEvent extends Event[PacketEventHooks, PacketEventListener] {
    protected type PacketEventHook = EventHook[PacketEventListener, this.type]
    val packet: Packet

    override def getHooks(category: PacketEventHooks): Array[PacketEventHook]
}
