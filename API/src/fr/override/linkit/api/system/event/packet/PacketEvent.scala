package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.system.event.{Event, EventHook}

trait PacketEvent extends Event[PacketEventListener] {
    protected type PacketEventHook = EventHook[this.type, PacketEventListener]
    val packet: Packet

    override def getHooks: Array[PacketEventHook]
}
