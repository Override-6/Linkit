package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait ImmediatePacketInjectable extends PacketInjectable {

    def addOnPacketInjected(action: (Packet, PacketCoordinates) => Unit): Unit

}
