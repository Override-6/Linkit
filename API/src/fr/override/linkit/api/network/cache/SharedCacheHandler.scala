package fr.`override`.linkit.api.network.cache

import java.util.NoSuchElementException

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.network.cache.SharedCacheHandler.MockCache
import fr.`override`.linkit.api.network.cache.map.SharedMap
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.WrappedPacket

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal


class SharedCacheHandler(family: String, ownerID: String)(implicit traffic: PacketTraffic) {

    private val communicator: CommunicationPacketCollector = traffic.openCollector(11, CommunicationPacketCollector.providable)
    private val subBroadcastChannel: CommunicationPacketChannel = communicator.subChannel("BROADCAST", CommunicationPacketChannel)
    private val relayID = traffic.ownerID
    private val cacheOwners: SharedMap[Int, String] = init()
    private val sharedObjects: SharedMap[Int, Any] = open(1, SharedMap[Int, Any])
    private val isHandlingSelf = ownerID == relayID

    this.synchronized {
        notifyAll() //Releases all awaitReady locks, mark as ready.
    }

    def post[A](key: Int, value: A): A = {
        sharedObjects.put(key, value)
        value
    }

    def get[A](key: Int): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]

    def apply[A](key: Int): A = sharedObjects.get(key).get.asInstanceOf[A]


    def open[A <: HandleableSharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A]): A = {
        //println(s"<$family, $ownerID> " + "Opening " + cacheID + " In " + Thread.currentThread() )

        LocalCacheHandler
                .findCache[A](cacheID)
                .getOrElse {
                    val baseContent: Array[Any] = retrieveBaseContent(cacheID)
                    val sharedCache = factory.createNew(family, cacheID, baseContent, subBroadcastChannel)
                    LocalCacheHandler.register(cacheID, sharedCache)
                    sharedCache
                }
    }

    private def retrieveBaseContent(cacheID: Int): Array[Any] = {
        //println(s"<$family> " + "RETRIEVING BASE CONTENT FOR " + cacheID + s"(${Thread.currentThread()})")
        //println(s"<$family> " + s"cacheOwners = ${cacheOwners}")
        if (isHandlingSelf || !cacheOwners.contains(cacheID)) {
            //println(s"<$family> " + "Does not exists, setting current relay as owner of this cache")
            cacheOwners.put(cacheID, relayID)
            return Array()
        }
        //println(s"<$family> " + "Retrieving cache...")
        val owner = cacheOwners(cacheID)
        val content = retrieveBaseContent(cacheID, owner)
        //println(s"<$family, $ownerID> " + "Content retrieved is : " + content.mkString("Array(", ", ", ")"))
        content
    }

    private def init(): SharedMap[Int, String] = {
        if (this.cacheOwners != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val content = retrieveBaseContent(-1, ownerID)

        val cacheOwners = SharedMap[Int, String].createNew(family, -1, content, subBroadcastChannel)
        LocalCacheHandler.register(-1, cacheOwners)
        cacheOwners
                .foreachKeys(LocalCacheHandler.registerMock) //mock all current caches that are registered on this family
                .addListener((_, key, _) => LocalCacheHandler.registerMock(key)) //Add a listener for all future registered caches that would not be registered locally
        ////println(s"Cache owners currently : $cacheOwners")
        cacheOwners
    }

    private def retrieveBaseContent(cacheID: Int, owner: String): Array[Any] = this.synchronized {
        if (cacheID == -1 && isHandlingSelf)
            return Array()
        ////println(s"family($family) " + s"Retrieving content id $cacheID to owner $owner (${Thread.currentThread()})" )
        communicator.sendRequest(WrappedPacket(family, DataPacket(s"$cacheID")), owner)
        ////println(s"family($family) " + s"waiting for response...")
        val o: Array[Any] = communicator.nextResponse(ObjectPacket).casted //The request will return the cache content
        ////println(s"family($family) " + "Response got !")
        o
    }

    private def initPacketHandling(): Unit = {
        communicator.addRequestListener((packet, coords) => {
            packet match {
                case WrappedPacket(tag, subPacket) => if (tag == family)
                    handlePacket(subPacket, coords)
            }
        })
    }

    private def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            //Normal packet
            case WrappedPacket(key, subPacket) =>
                awaitReady()
                LocalCacheHandler.injectPacket(key.toInt, subPacket, coords)

            //Cache initialisation packet
            case DataPacket(cacheIdentifier, _) =>
                val cacheID: Int = cacheIdentifier.toInt
                val senderID: String = coords.senderID

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

    private def awaitReady(): Unit = {
        def notReady: Boolean = cacheOwners == null || cacheOwners.isEmpty
        if (notReady) this.synchronized {
            RelayWorkerThreadPool.smartWait(this, notReady)
        }
    }

    protected object LocalCacheHandler {

        private val localRegisteredCaches = mutable.Map.empty[Int, HandleableSharedCache]

        def register(identifier: Int, cache: HandleableSharedCache): Unit = {
            localRegisteredCaches.put(identifier, cache)
        }

        def injectPacket(key: Int, packet: Packet, coords: PacketCoordinates): Unit = try {
            localRegisteredCaches(key).handlePacket(packet, coords)
        } catch {
            case _: NoSuchElementException =>
                registerMock(key)
            case NonFatal(e) => e.printStackTrace(Console.out)
        }

        def registerMock(identifier: Int): Unit = {
            localRegisteredCaches.put(identifier, MockCache)
        }

        def getContent(cacheID: Int): Array[Any] = {
            localRegisteredCaches(cacheID).currentContent
        }

        def findCache[A : ClassTag](cacheID: Int): Option[A] = {
            val opt = localRegisteredCaches.get(cacheID).asInstanceOf[Option[A]]
            if (opt.exists(_.isInstanceOf[MockCache.type]))
                return None

            if (opt.exists(_.isInstanceOf[A])) {
                val requestedClass = classTag[A].runtimeClass
                val presentClass = opt.get.getClass
                throw new IllegalArgumentException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. ($presentClass vs $requestedClass)")
            }

            opt
        }
    }


}

object SharedCacheHandler {

    def create(identifier: String, ownerID: String)(implicit traffic: PacketTraffic): SharedCacheHandler = {
        new SharedCacheHandler(identifier, ownerID)(traffic)
    }

    object MockCache extends HandleableSharedCache("", -1, null) {
        override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = ()

        override def currentContent: Array[Any] = Array()

        override var autoFlush: Boolean = false

        override def flush(): MockCache.this.type = this

        override def modificationCount(): Int = -1
    }

}