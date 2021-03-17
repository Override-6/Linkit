package fr.`override`.linkit.core.internal.system.event.packet

import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.internal.system.event.{Event, EventHook}

trait PacketEvent extends Event[PacketEventHooks, PacketEventListener] {
    protected type PacketEventHook = EventHook[PacketEventListener, this.type]
    val packet: Packet

    override def getHooks(category: PacketEventHooks): Array[PacketEventHook]
}
