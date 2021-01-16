package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait SharedCacheNotifier extends SharedCache {

    def notifyPacket(packet: Packet, coords: PacketCoordinates)

}
