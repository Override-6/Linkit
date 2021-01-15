package fr.`override`.linkit.api.packet.traffic

trait DedicatedPacketInjectable extends PacketInjectable {
    val connectedID: String
}
