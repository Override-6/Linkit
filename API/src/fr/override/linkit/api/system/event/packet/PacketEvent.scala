package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.system.event.{Event, EventHook}

trait PacketEvent extends Event[PacketEventHooks, PacketEventListener] {
    protected type PacketEventHook = EventHook[PacketEventListener, this.type]
    val packet: Packet

    override def getHooks(category: PacketEventHooks): Array[PacketEventHook]
}
