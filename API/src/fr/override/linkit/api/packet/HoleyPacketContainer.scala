package fr.`override`.linkit.api.packet

trait HoleyPacketContainer extends PacketContainer {

    def onPacketReceived(action: (Packet, PacketCoordinates) => Unit): Unit

}
