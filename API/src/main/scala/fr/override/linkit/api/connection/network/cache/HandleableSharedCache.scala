package fr.`override`.linkit.api.connection.network.cache

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

trait HandleableSharedCache extends SharedCache {

    def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def currentContent: Array[Any]

}
