package fr.`override`.linkit.api.packet

trait ImmediatePacketInjectable extends PacketInjectable {

    def onPacketInjected(action: (Packet, PacketCoordinates) => Unit): Unit

}
