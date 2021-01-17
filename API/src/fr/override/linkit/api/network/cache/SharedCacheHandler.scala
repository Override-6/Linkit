package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.network.cache.map.SharedMap
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable

abstract class SharedCacheHandler(ownerID: String) {

    protected val subBroadcastChannel: CommunicationPacketChannel
    private val openedCaches = init()
    private val sharedGlobalObjects = open(1, SharedMap[Int, Any])

    def post(key: Int, value: Any): Unit = sharedGlobalObjects.put(key, value)

    def get[A](key: Int): Option[A] = sharedGlobalObjects.get(key).asInstanceOf[Option[A]]

    def apply[A](key: Int): A = sharedGlobalObjects.get(key).get.asInstanceOf[A]

    def open[A <: HandleableSharedCache](cacheID: Int, factory: SharedCacheFactory[A]): A = {
        println("Opening " + cacheID + " In " + getClass.getSimpleName)
        val baseContent: Array[AnyRef] = retrieveBaseContent(cacheID)
        val sharedCache = factory.createNew(cacheID, baseContent, subBroadcastChannel)
        LocalCacheHandler.register(cacheID, sharedCache)
        sharedCache
    }

    private def retrieveBaseContent(cacheID: Int): Array[AnyRef] = {
        if (!openedCaches.contains(cacheID)) {
            openedCaches.put(cacheID, ownerID)
            return Array()
        }
        val owner = openedCaches(cacheID)
        retrieveBaseContent(cacheID, owner)
    }

    protected def retrieveBaseContent(cacheID: Int, owner: String): Array[AnyRef]

    protected def initPacketHandling(): Unit

    protected def sendContent(content: Array[Any], targetID: String): Unit

    final protected def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        println("Received " + packet + " In " + getClass.getSimpleName)
        packet match {
            //Normal packet
            case WrappedPacket(key, subPacket) =>
                LocalCacheHandler.injectPacket(key.toInt, subPacket, coords)

            //Cache initialisation packet
            case DataPacket(cacheIdentifier, _) =>
                val cacheID: Int = cacheIdentifier.toInt
                val senderID: String = coords.senderID
                if (cacheID != -1 && openedCaches(cacheID) != ownerID)
                    throw new UnexpectedPacketException("Attempted to retrieve a cache content from this relay, but this relay isn't the current owner of this cache.")

                val content = if (cacheID == -1) {
                    if (openedCaches == null) {
                        Array[Any]()
                    }
                    else openedCaches.currentContent
                }
                else LocalCacheHandler.getContent(cacheID)

                sendContent(content, senderID)
        }
    }

    private def init(): SharedMap[Int, String] = {
        if (this.openedCaches != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val content = retrieveBaseContent(-1, Relay.ServerIdentifier)

        val openedCaches = SharedMap[Int, String].createNew(-1, content, subBroadcastChannel)
        LocalCacheHandler.register(-1, openedCaches)
        openedCaches
    }

    protected object LocalCacheHandler {
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

object SharedCacheHandler {

    def dedicated(targetID: String)(implicit traffic: PacketTraffic): SharedCacheHandler = {
        new Dedicated(targetID)(traffic)
    }

    def global(implicit traffic: PacketTraffic): SharedCacheHandler = {
        new Global()(traffic)
    }

    class Dedicated private[SharedCacheHandler](targetID: String)(implicit traffic: PacketTraffic) extends SharedCacheHandler(traffic.ownerID) {

        override protected lazy val subBroadcastChannel: CommunicationPacketChannel = communicator
        private lazy val communicator = traffic.openChannel(12, targetID, CommunicationPacketChannel)

        override protected def retrieveBaseContent(cacheID: Int, ignored: String): Array[AnyRef] = {
            if (cacheID == -1)
                return Array() //FIXME May not work for reconnections. To fix that, a specific packet may be send directly to the server
            communicator.sendRequest(DataPacket(s"$cacheID"))
            communicator.nextResponse(ObjectPacket).casted //The request will return the cache content
        }

        override protected def initPacketHandling(): Unit = {
            communicator.addRequestListener(handlePacket)
        }

        override protected def sendContent(content: Array[Any], ignored: String): Unit = {
            communicator.sendResponse(ObjectPacket(content))
        }
    }

    class Global private[SharedCacheHandler](implicit traffic: PacketTraffic) extends SharedCacheHandler(traffic.ownerID) {

        override protected lazy val subBroadcastChannel: CommunicationPacketChannel = communicator.subChannel("BROADCAST", CommunicationPacketChannel, true)
        private lazy val communicator = traffic.openCollector(11, CommunicationPacketCollector)

        override protected def retrieveBaseContent(cacheID: Int, owner: String): Array[AnyRef] = {
            if (cacheID == -1 && traffic.ownerID == Relay.ServerIdentifier)
                return Array()
            communicator.sendRequest(DataPacket(s"$cacheID"), owner)
            communicator.nextResponse(ObjectPacket).casted //The request will return the cache content
        }

        override protected def initPacketHandling(): Unit = communicator.addRequestListener(handlePacket)

        override protected def sendContent(content: Array[Any], targetID: String): Unit = {
            communicator.sendResponse(ObjectPacket(content), targetID)
        }
    }

}
