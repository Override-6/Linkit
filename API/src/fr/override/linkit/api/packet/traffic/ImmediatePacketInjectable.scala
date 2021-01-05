package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait ImmediatePacketInjectable extends PacketInjectable {

    def onPacketInjected(action: (Packet, PacketCoordinates) => Unit): Unit

}
