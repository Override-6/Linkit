package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.concurrency.RelayThreadPool
import fr.`override`.linkit.api.network.cache.SharedCacheHandler.MockCache
import fr.`override`.linkit.api.network.cache.map.SharedMap
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.api.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet, PacketCoordinates}

import java.util.NoSuchElementException
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

//FIXME: Critical bug occurred when a lot of clients are connecting to the server,
// packets begin to shift, they are injected multiple times (maybe due to packet coordinates(id/senderID) ambiguity into the PacketInjections class)
// and this is a big problem for this class to initialise completely, which is a big problem for the network's initialisation,
// which is a big problem for the relay's initialisation....

//TODO Use Array[Serializable] instead of Array[Any] for shared contents
//TODO replace Longs with Ints (be aware that, with the current serialization algorithm, primitives integers are all converted to Long, so it would cause cast problems)
class SharedCacheHandler(val family: String, val ownerID: String)(implicit traffic: PacketTraffic) {

    private val communicator = traffic.getInjectable(11, ChannelScope.broadcast, CommunicationPacketChannel.providable)
    println(s"Damny communicator = ${communicator}")

    private val relayID = traffic.relayID
    private val isHandlingSelf = ownerID == relayID

    private val cacheOwners: SharedMap[Long, String] = init()
    private val sharedObjects: SharedMap[Long, Serializable] = get(1, SharedMap[Long, Serializable])
    println(s"sharedObjects = ${sharedObjects}")

    this.synchronized {
        notifyAll() //Releases all awaitReady locks, this action is marking this cache handler as ready.
    }

    def post[A <: Serializable](key: Long, value: A): A = {
        sharedObjects.put(key, value)
        value
    }

    def get[A <: Serializable](key: Long): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]

    def apply[A <: Serializable](key: Long): A = {
        println(s"getting shared instance for key $key")
        get(key).get
    }

    def get[A <: HandleableSharedCache[_] : ClassTag](cacheID: Long, factory: SharedCacheFactory[A]): A = {
        LocalCacheHandler
                .findCache[A](cacheID)
                .fold {
                    println(s"OPENING CACHE $cacheID OF TYPE ${classTag[A].runtimeClass}")
                    val baseContent = retrieveBaseContent(cacheID)
                    println(s"CONTENT RECEIVED (${baseContent.mkString("Array(", ", ", ")")}) FOR CACHE $cacheID")
                    val sharedCache = factory.createNew(this, cacheID, baseContent, communicator)
                    LocalCacheHandler.register(cacheID, sharedCache)
                    sharedCache
                }(_.update())
    }

    def forget(cacheID: Long): Unit = {
        LocalCacheHandler.unregister(cacheID)
    }

    def update(): this.type = {
        println(s"cacheOwners = ${cacheOwners}")
        cacheOwners.update()
        println(s"cacheOwners = ${cacheOwners}")
        LocalCacheHandler.updateAll()
        //sharedObjects will be updated by LocalCacheHandler.updateAll
        this
    }

    def getOwner(cacheID: Long): String = {
        cacheOwners
                .get(cacheID)
                .getOrElse(ownerID)
    }

    private def retrieveBaseContent(cacheID: Long): Array[Any] = {
        println(s"RETRIEVING BASE CONTENT FOR CACHE num $cacheID")
        println(s"cacheOwners = ${cacheOwners}")
        if (!cacheOwners.contains(cacheID)) {
            println(s"Does not exists, setting current relay as owner of this cache.")
            cacheOwners.put(cacheID, relayID)
            println(s"cacheOwners are now ${cacheOwners}")
            return Array()
        }
        val owner = cacheOwners(cacheID)
        println(s"owner is $owner")
        val content = retrieveRemoteBaseContent(cacheID, owner)
        content
    }

    private def retrieveRemoteBaseContent(cacheID: Long, owner: String): Array[Any] = LocalCacheHandler.synchronized {
        if (cacheID == -1 && isHandlingSelf)
            return Array((1L, ownerID))

        println(s"Sending request to owner $owner in order to retrieve content of cache number $cacheID")
        communicator.sendRequest(WrappedPacket(family, LongPacket(cacheID)), owner)
        println(s"request sent !")
        val content = communicator.nextResponse[ArrayObjectPacket].value //The request will return the cache content
        println(s"Content received ! (${content.mkString("Array(", ", ", ")")})")
        content.asInstanceOf[Array[Any]]
    }

    private def init(): SharedMap[Long, String] = {
        if (this.cacheOwners != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val content = retrieveRemoteBaseContent(-1, ownerID)

        val cacheOwners = SharedMap[Long, String].createNew(this, -1, content, communicator)
        LocalCacheHandler.register(-1L, cacheOwners)
        println(s"cacheOwners = ${cacheOwners}")
        cacheOwners
                .foreachKeys(LocalCacheHandler.registerMock) //mock all current caches that are registered on this family
        cacheOwners
    }

    private def initPacketHandling(): Unit = {
        communicator.addRequestListener((packet, coords) => {
            packet match {
                case WrappedPacket(tag, subPacket) =>
                    if (tag == family)
                        handlePacket(subPacket, coords)
            }
        })
    }


    private def handlePacket(packet: Packet, coords: DedicatedPacketCoordinates): Unit = {
        println(s"HANDLING PACKET ${packet}, $coords")

        awaitReady()
        packet match {
            //Normal packet
            case WrappedPacket(key, subPacket) =>
                LocalCacheHandler.injectPacket(key.toLong, subPacket, coords)

            //Cache initialisation packet
            case LongPacket(cacheID) =>
                val senderID: String = coords.senderID
                println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
                val content = if (cacheID == -1) {
                    if (cacheOwners == null) {
                        Array[Any]()
                    }
                    else cacheOwners.currentContent
                }
                else LocalCacheHandler.getContent(cacheID)
                println(s"Content = ${content.mkString("Array(", ", ", ")")}")
                communicator.sendResponse(ArrayObjectPacket(content), senderID)
                println("Packet sent :D")
        }
    }

    private def awaitReady(): Unit = {
        def notReady: Boolean = cacheOwners == null || cacheOwners.isEmpty

        if (notReady) this.synchronized {
            RelayThreadPool.executeRemainingTasks(this, notReady)
        }
    }

    protected object LocalCacheHandler {

        private val localRegisteredCaches = mutable.Map.empty[Long, HandleableSharedCache[_]]

        def updateAll(): Unit = {
            println(s"updating cache ($localRegisteredCaches)...")
            localRegisteredCaches
                    .foreach(_._2.update())
            println(s"cache updated ! ($localRegisteredCaches)")
        }

        def register(identifier: Long, cache: HandleableSharedCache[_]): Unit = {
            println(s"Registering $identifier into local cache.")
            localRegisteredCaches.put(identifier, cache)
            println(s"Local cache is now $localRegisteredCaches")
        }

        def unregister(identifier: Long): Unit = {
            println(s"Removing cache $identifier")
            localRegisteredCaches.remove(identifier)
            println(s"Cache is now $identifier")
        }

        def injectPacket(key: Long, packet: Packet, coords: PacketCoordinates): Unit = try {
            localRegisteredCaches(key).handlePacket(packet, coords)
        } catch {
            case _: NoSuchElementException =>
                registerMock(key)
            case NonFatal(e) => e.printStackTrace(Console.out)
        }

        def registerMock(identifier: Long): Unit = {
            localRegisteredCaches.put(identifier, MockCache)
        }

        def getContent(cacheID: Long): Array[Any] = {
            localRegisteredCaches(cacheID).currentContent
        }

        def findCache[A: ClassTag](cacheID: Long): Option[A] = {
            val opt = localRegisteredCaches.get(cacheID).asInstanceOf[Option[A]]
            if (opt.exists(_.isInstanceOf[MockCache.type]))
                return None

            if (opt.exists(!_.isInstanceOf[A])) {
                val requestedClass = classTag[A].runtimeClass
                val presentClass = opt.get.getClass
                throw new IllegalArgumentException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. ($presentClass vs $requestedClass)")
            }

            opt
        }

        override def toString: String = localRegisteredCaches.toString()

    }

    private def println(msg: String): Unit = {
        Console.println(s"<$family, $ownerID> $msg")
    }

}

object SharedCacheHandler {

    private val caches = mutable.HashMap.empty[(String, String, PacketTraffic), SharedCacheHandler]

    def get(identifier: String, ownerID: String)(implicit traffic: PacketTraffic): SharedCacheHandler = {
        caches.get((identifier, ownerID, traffic))
                .fold {
                    println(s"--> CREATING SHARED CACHE HANDLER <$identifier, $ownerID>")
                    val cache = new SharedCacheHandler(identifier, ownerID)(traffic)
                    println(s"--> SHARED CACHE HANDLER CREATED <$identifier, $ownerID>")
                    caches.put((identifier, ownerID, traffic), cache)
                    cache
                } (cache => {
                    println(s"--> UPDATING CACHE <$identifier, $ownerID> INSTEAD OF CREATING IT.")
                    cache.update()
                    println(s"--> UPDATED CACHE <$identifier, $ownerID> INSTEAD OF CREATING IT.")
                    cache
                })
    }

    object MockCache extends HandleableSharedCache[Nothing](null, -1, null) {

        override val family: String = ""

        override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = ()

        override def currentContent: Array[Any] = Array()

        override var autoFlush: Boolean = false

        override def flush(): this.type = this

        override def modificationCount(): Int = -1

        override def update(): this.type = this

        override protected def setCurrentContent(content: Array[Nothing]): Unit = ()
    }

}