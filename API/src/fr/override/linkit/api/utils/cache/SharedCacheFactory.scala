package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.channel.PacketChannel

trait SharedCacheFactory[A] {

    def createNew(identifier: Int, baseContent: Array[Any], channel: PacketChannel): A

    def sharedCacheClass: Class[A]

    final def factory: this.type = this //for Java users

}
