package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket
import fr.`override`.linkit.api.utils.cache.map.SharedMap

import scala.collection.mutable

class SharedCacheHandler(implicit traffic: PacketTraffic) {

    private val communicator = traffic.openCollector(11, CommunicationPacketCollector)
    private val openedCaches = init()
    private val sharedGlobalObjects = open(1, SharedMap[Int, Any])

    def post(key: Int, value: Any): Unit = sharedGlobalObjects.put(key, value)

    def get[A](key: Int): Option[A] = sharedGlobalObjects.get(key).asInstanceOf[Option[A]]

    def apply[A](key: Int): A = sharedGlobalObjects.get(key).get.asInstanceOf[A]

    def open[A <: HandleableSharedCache](cacheID: Int, factory: SharedCacheFactory[A]): A = {
        val baseContent: Array[AnyRef] = retrieveBaseContent(cacheID)
        val channel = communicator.subChannel("BROADCAST", CommunicationPacketChannel, true)
        val sharedCache = factory.createNew(cacheID, baseContent, channel)
        LocalCacheHandler.register(cacheID, sharedCache)
        sharedCache
    }

    private def retrieveBaseContent(collectionIdentifier: Int): Array[AnyRef] = {
        if (!openedCaches.contains(collectionIdentifier)) {
            openedCaches.put(collectionIdentifier, traffic.ownerID)
            return Array()
        }
        val owner = openedCaches(collectionIdentifier)
        communicator.sendRequest(DataPacket(s"$collectionIdentifier"), owner)
        communicator.nextResponse(ObjectPacket).casted //The request will return the cache content
    }

    private def init(): SharedMap[Int, String] = {
        if (this.openedCaches != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val channel = communicator.subChannel("BROADCAST", CommunicationPacketChannel, true)

        val content: Array[AnyRef] = {
            if (traffic.ownerID != Relay.ServerIdentifier) {
                communicator.sendRequest(DataPacket(s"${-1}"), Relay.ServerIdentifier)
                communicator.nextResponse(ObjectPacket).casted
            } else Array()
        }

        val openedCaches = SharedMap[Int, String].createNew(-1, content, channel)
        LocalCacheHandler.register(-1, openedCaches)
        openedCaches
    }

    private def initPacketHandling(): Unit = {
        communicator.addRequestListener((packet, coords) => {

            packet match {
                //Normal packet
                case WrappedPacket(key, subPacket) =>
                    LocalCacheHandler.injectPacket(key.toInt, subPacket, coords)

                //Cache initialisation packet
                case DataPacket(cacheIdentifier, _) =>
                    val cacheID: Int = cacheIdentifier.toInt
                    val senderID: String = coords.senderID
                    if (cacheID != -1 && openedCaches(cacheID) != traffic.ownerID)
                        throw new UnexpectedPacketException("Attempted to retrieve a cache content from this relay, but this relay isn't the current owner of this cache.")

                    val content = if (cacheID == -1) openedCaches.currentContent else LocalCacheHandler.getContent(cacheID)
                    communicator.sendResponse(ObjectPacket(content), senderID)
            }
        })
    }

    private object LocalCacheHandler {
        private val localRegisteredCaches = mutable.Map.empty[Int, HandleableSharedCache]

        def register(identifier: Int, cache: HandleableSharedCache): Unit = {
            localRegisteredCaches.put(identifier, cache)
        }

        def injectPacket(key: Int, packet: Packet, coords: PacketCoordinates): Unit = {
            localRegisteredCaches(key).handlePacket(packet, coords)
        }

        def getContent(cacheID: Int): Array[Any] = {
            localRegisteredCaches(cacheID).currentContent
        }

    }

}
