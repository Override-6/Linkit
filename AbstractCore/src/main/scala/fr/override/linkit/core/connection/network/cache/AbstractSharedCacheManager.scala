package fr.`override`.linkit.core.connection.network.cache

import fr.`override`.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketSender, PacketTraffic}
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketCoordinates}
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager.{MockCache, RequestSender}
import fr.`override`.linkit.core.connection.network.cache.map.SharedMap
import fr.`override`.linkit.core.connection.packet.UnexpectedPacketException
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel

import java.util.NoSuchElementException
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

//FIXME: Critical bug occurred when a lot of clients are connecting to the server,
// packets begin to shift, they are injected multiple times (maybe due to packet coordinates(id/senderID) ambiguity into the PacketInjections class)
// and this is a big problem for this class to initialise completely, which is a big problem for the network's initialisation,
// which is a big problem for the client relay's initialisation....

//TODO Use Array[Serializable] instead of Array[Any] for shared contents
//TODO replace Longs with Ints (be aware that, with the current serialization algorithm,
// primitives integers are all converted to Long, so it would cause cast problems until the algorithm is modified)
abstract class AbstractSharedCacheManager(val family: String, ownerIdentifier: String, traffic: PacketTraffic) extends SharedCacheManager {

    protected val communicator: RequestSender =
        traffic.getInjectable(11, ChannelScope.broadcast, new RequestSender(_))

    private val sharedObjects: map.SharedMap[Long, Serializable] = init()
    println(s"sharedObjects = ${sharedObjects}")

    override def post[A <: Serializable](key: Long, value: A): A = {
        sharedObjects.put(key, value)
        value
    }

    override def get[A <: Serializable](key: Long): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]

    override def getOrWait[A <: Serializable](key: Long): A = sharedObjects.getOrWait(key).asInstanceOf[A]

    override def apply[A <: Serializable](key: Long): A = sharedObjects(key).asInstanceOf[A]

    override def get[A <: HandleableSharedCache[_] : ClassTag](cacheID: Long, factory: SharedCacheFactory[A]): A = {
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

    override def update(): this.type = {
        LocalCacheHandler.updateAll()
        //sharedObjects will be updated by LocalCacheHandler.updateAll
        this
    }

    private def retrieveBaseContent(cacheID: Long): Array[Any] = {
        println(s"Sending request to server in order to retrieve content of cache number $cacheID")
        communicator.sendRequest(WrappedPacket(family, LongPacket(cacheID)), ownerIdentifier)
        println(s"request sent !")
        val content = communicator.nextResponse[ArrayObjectPacket].value //The request will return the cache content
        println(s"Content received ! (${content.mkString("Array(", ", ", ")")})")
        content.asInstanceOf[Array[Any]]
    }

    private def init(): SharedMap[Long, Serializable] = {
        if (this.sharedObjects != null)
            throw new IllegalStateException("This SharedCacheManager is already initialised !")

        initPacketHandling()

        val content = retrieveBaseContent(1)

        val cacheOwners = SharedMap[Long, Serializable].createNew(this, 1, content, communicator)
        LocalCacheHandler.register(1L, cacheOwners)
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

    def continuePacketHandling(packet: Packet, coords: DedicatedPacketCoordinates): Unit

    private def handlePacket(packet: Packet, coords: DedicatedPacketCoordinates): Unit = {
        println(s"HANDLING PACKET ${packet}, $coords")

        packet match {
            //Normal packet
            case WrappedPacket(key, subPacket) =>
                LocalCacheHandler.injectPacket(key.toLong, subPacket, coords)

            case _ => continuePacketHandling(packet, coords)
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

        def injectPacket(cacheID: Long, packet: Packet, coords: PacketCoordinates): Unit = try {
            localRegisteredCaches(cacheID).handlePacket(packet, coords)
        } catch {
            case _: NoSuchElementException =>
                println(s"Mocked $cacheID")
                registerMock(cacheID)
            case NonFatal(e) => e.printStackTrace(Console.out)
        }

        def registerMock(identifier: Long): Unit = {
            localRegisteredCaches.put(identifier, MockCache)
        }

        def getContent(cacheID: Long): Array[Any] = {
            localRegisteredCaches(cacheID).currentContent
        }

        def getContentOrElseMock(cacheID: Long): Array[Any] = {
            val opt = localRegisteredCaches.get(cacheID)
            if (opt.isEmpty) {
                registerMock(cacheID)
                return Array()
            }
            opt.get.currentContent
        }

        def isRegistered(cacheID: Long): Boolean = {
            localRegisteredCaches.contains(cacheID)
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
        Console.println(s"<$family, $ownerIdentifier> $msg")
    }

}

object AbstractSharedCacheManager {

    private val caches = mutable.HashMap.empty[(String, PacketTraffic), AbstractSharedCacheManager]

    def get(family: String, ownerIdentifier: String,
            factory: (String, String, PacketTraffic) => AbstractSharedCacheManager = new DefaultSharedCacheManager(_, _, _))
           (implicit traffic: PacketTraffic): AbstractSharedCacheManager = {

        caches.get((family, traffic))
                .fold {
                    println(s"--> CREATING SHARED CACHE HANDLER <$family>")
                    val cache = factory(family, ownerIdentifier, traffic)
                    println(s"--> SHARED CACHE HANDLER CREATED <$family>")
                    caches.put((family, traffic), cache)
                    cache
                }(cache => {
                    println(s"--> UPDATING CACHE <$family> INSTEAD OF CREATING IT.")
                    cache.update()
                    println(s"--> UPDATED CACHE <$family> INSTEAD OF CREATING IT.")
                    cache
                })
    }

    class DefaultSharedCacheManager private[AbstractSharedCacheManager](family: String, owner: String, traffic: PacketTraffic)
            extends AbstractSharedCacheManager(family, owner: String, traffic) {

        override def continuePacketHandling(packet: Packet, coords: DedicatedPacketCoordinates): Unit = {
            throw UnexpectedPacketException("Received forbidden/not handleable packet for this shared cache handler")
        }
    }

    private class RequestSender(scope: ChannelScope) extends CommunicationPacketChannel(scope, true) with PacketSender {
        override def send(packet: Packet): Unit = sendRequest(packet)

        override def sendTo(packet: Packet, targets: String*): Unit = sendRequest(packet, targets: _*)
    }

    object MockCache extends HandleableSharedCache[Nothing](null, -1, null) {

        override val family: String = ""

        override var autoFlush: Boolean = false

        override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = ()

        override def currentContent: Array[Any] = Array()

        override def flush(): this.type = this

        override def modificationCount(): Int = -1

        override def update(): this.type = this

        override protected def setCurrentContent(content: Array[Nothing]): Unit = ()
    }

}