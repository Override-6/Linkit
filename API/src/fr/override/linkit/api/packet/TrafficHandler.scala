package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.system.{JustifiedCloseable, Reason}

trait TrafficHandler extends JustifiedCloseable {

    val relayID: String

    def register(container: PacketContainer): Unit

    def unregister(id: Int, reason: Reason): Unit

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def sendPacket(packet: Packet, coords: PacketCoordinates): Unit

    def sendPacket(packet: Packet, identifier: Int, targetID: String): Unit

    def isRegistered(identifier: Int): Boolean

    def checkThread(): Unit

    //def notifyPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit

}