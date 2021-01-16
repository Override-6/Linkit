package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.utils.WrappedPacket
import fr.`override`.linkit.api.utils.cache.collection.SharedCollection
import fr.`override`.linkit.api.utils.cache.map.SharedMap

import scala.collection.mutable

class SharedCaches(implicit traffic: PacketTraffic) {

    private val sharedGlobalObjects: SharedMap[Int, Any] = SharedMap.open(10)
    private val openedCaches = open(-1, SharedCollection[Int])

    def post(key: Int, value: Any): Unit = sharedGlobalObjects.put(key, value)

    def get(key: Int): Unit = sharedGlobalObjects.get(key)

    def open[A <: SharedCacheNotifier, B <: SharedCache](collectionIdentifier: Int, factory: SharedCacheFactory[A]): B = {
        val baseContent: Array[Any] = retrieveBaseContent(collectionIdentifier)
        val sharedCache = factory.createNew(collectionIdentifier, baseContent, null) //TODO
        LocalSharedCacheHandler.register(collectionIdentifier, sharedCache)
        sharedCache.asInstanceOf[B]
    }

    def retrieveBaseContent(collectionIdentifier: Int): Array[Any] = ???

    private object LocalSharedCacheHandler {
        private val communicator = traffic.openCollector(11, CommunicationPacketCollector)
        private val registeredCaches = mutable.Map.empty[Int, SharedCache with SharedCacheNotifier]

        communicator.addRequestListener((packet, coords) => {
            packet match {
                case WrappedPacket(key, subPacket) =>
                    registeredCaches(key.toInt).notifyPacket(subPacket, coords)
            }
        })

        def register(identifier: Int, cache: SharedCache with SharedCacheNotifier): Unit = {
            registeredCaches.put(identifier, cache)
        }

    }

}
