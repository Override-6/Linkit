package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.network.cache.map.SharedMap
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable

class SharedCacheHandler(family: String, ownerID: String)(implicit traffic: PacketTraffic) {

    private val communicator: CommunicationPacketCollector = traffic.openCollector(11, CommunicationPacketCollector)
    private val subBroadcastChannel: CommunicationPacketChannel = communicator.subChannel("BROADCAST", CommunicationPacketChannel)
    private val cacheOwners: SharedMap[Int, String] = init()
    private val sharedObjects: SharedMap[Int, Any] = open(1, SharedMap[Int, Any])
    private val isHandlingSelf = ownerID == traffic.ownerID

    def post(key: Int, value: Any): Unit = sharedObjects.put(key, value)

    def get[A](key: Int): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]

    def apply[A](key: Int): A = sharedObjects.get(key).get.asInstanceOf[A]

    def open[A <: HandleableSharedCache](cacheID: Int, factory: SharedCacheFactory[A]): A = {
        //println(s"family($family) " + "Opening " + cacheID + " In " + getClass.getSimpleName)
        val baseContent: Array[AnyRef] = retrieveBaseContent(cacheID)
        val sharedCache = factory.createNew(family, cacheID, baseContent, subBroadcastChannel)
        LocalCacheHandler.register(cacheID, sharedCache)
        sharedCache
    }

    private def retrieveBaseContent(cacheID: Int): Array[AnyRef] = {
        println(s"<$family> " + "RETRIEVING BASE CONTENT FOR " + cacheID)
        println(s"<$family> " + s"cacheOwners = ${cacheOwners}")
        if (isHandlingSelf || !cacheOwners.contains(cacheID)) {
            println(s"<$family> " + "Does not exists, setting current relay as owner of this cache")
            cacheOwners.put(cacheID, ownerID)
            return Array()
        }
        println(s"<$family> " + "Retrieving cache...")
        val owner = cacheOwners(cacheID)
        val content = retrieveBaseContent(cacheID, owner)
        println(s"<$family> " + "Content retrieved is : " + content.mkString("Array(", ", ", ")"))
        content
    }

    private def init(): SharedMap[Int, String] = {
        if (this.cacheOwners != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val content = retrieveBaseContent(-1, ownerID)

        val openedCaches = SharedMap[Int, String].createNew(family, -1, content, subBroadcastChannel)
        LocalCacheHandler.register(-1, openedCaches)
        openedCaches
    }

    private def retrieveBaseContent(cacheID: Int, owner: String): Array[AnyRef] = {
        if (cacheID == -1 && isHandlingSelf)
            return Array()
        //println(s"family($family) " + s"Retrieving content id $cacheID to owner $owner")
        communicator.sendRequest(WrappedPacket(family, DataPacket(s"$cacheID")), owner)
        //println(s"family($family) " + s"waiting for response...")
        val o: Array[AnyRef] = communicator.nextResponse(ObjectPacket).casted //The request will return the cache content
        //println(s"family($family) " + "Response got !")
        o
    }

    private def initPacketHandling(): Unit = {
        communicator.addRequestListener((packet, coords) => {
            //println(s"family($family) " + "Testing " + packet + " for familly " + family)
            packet match {
                case WrappedPacket(tag, subPacket) => if (tag == family)
                    handlePacket(subPacket, coords)
            }
        })
    }

    private def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        //println(s"family($family) Received " + packet + " In " + getClass.getSimpleName)
        packet match {
            //Normal packet
            case WrappedPacket(key, subPacket) =>
                LocalCacheHandler.injectPacket(key.toInt, subPacket, coords)

            //Cache initialisation packet
            case DataPacket(cacheIdentifier, _) =>
                val cacheID: Int = cacheIdentifier.toInt
                val senderID: String = coords.senderID
                if (cacheID != -1 && cacheOwners(cacheID) != ownerID)
                    throw new UnexpectedPacketException("Attempted to retrieve a cache content from this relay, but this relay isn't the current owner of this cache.")

                val content = if (cacheID == -1) {
                    if (cacheOwners == null) {
                        Array[Any]()
                    }
                    else cacheOwners.currentContent
                }
                else LocalCacheHandler.getContent(cacheID)

                communicator.sendResponse(ObjectPacket(content), senderID)
        }
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

    def create(identifier: String, ownerID: String)(implicit traffic: PacketTraffic): SharedCacheHandler = {
        new SharedCacheHandler(identifier, ownerID)(traffic)
    }

}
