package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel

trait SharedCacheFactory[A <: SharedCache] {

    def createNew(family: String, identifier: Int, baseContent: Array[AnyRef], channel: CommunicationPacketChannel): A

    def sharedCacheClass: Class[A]

    final def factory: this.type = this //for Java users

}
