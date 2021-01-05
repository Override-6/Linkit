package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}


trait PacketTraffic extends PacketSender with JustifiedCloseable {

    def register(injectable: PacketInjectable): Unit

    def unregister(id: Int, reason: CloseReason): Unit

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def isRegistered(identifier: Int): Boolean

}
