package fr.`override`.linkit.api.system.event.packet

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.system.event.Event

trait PacketEvent extends Event[PacketEventListener] {
    val packet: Packet
}
