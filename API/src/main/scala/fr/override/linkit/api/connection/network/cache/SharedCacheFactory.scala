package fr.`override`.linkit.api.connection.network.cache

import fr.`override`.linkit.api.connection.packet.traffic.PacketSender

trait SharedCacheFactory[A <: SharedCache] {

    def createNew(handler: SharedCacheManager, identifier: Long, baseContent: Array[Any], channel: PacketSender): A

    final def factory: this.type = this //for Java users

}
