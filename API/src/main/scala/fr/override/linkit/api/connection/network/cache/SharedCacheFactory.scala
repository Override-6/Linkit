package fr.`override`.linkit.api.connection.network.cache

import fr.`override`.linkit.api.connection.packet.traffic.channel.CommunicationPacketChannel

trait SharedCacheFactory[A <: SharedCache] {

    def createNew(handler: SharedCacheHandler, identifier: Long, baseContent: Array[Any], channel: CommunicationPacketChannel): A

    final def factory: this.type = this //for Java users

}
