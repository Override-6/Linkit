package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.system.{JustifiedCloseable, Reason}

trait TrafficHandler extends JustifiedCloseable {

    val relayID: String

    def register(container: PacketContainer): Unit

    def unregister(id: Int, reason: Reason): Unit

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def sendPacket(packet: Packet, coords: PacketCoordinates): Unit

    def sendPacket(packet: Packet, identifier: Int, targetID: String): Unit

    def isTargeted(identifier: Int): Boolean

    def isTargeted(coordinates: PacketCoordinates): Boolean = isTargeted(coordinates.containerID)

    //def notifyPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit

}