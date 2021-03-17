package fr.`override`.linkit.core.connection.network.cache

import fr.`override`.linkit.skull.Relay.ServerIdentifier
import fr.`override`.linkit.skull.connection.network.Updatable
import fr.`override`.linkit.skull.connection.network.cache.SharedCacheHandler.MockCache
import fr.`override`.linkit.skull.connection.network.cache.map.SharedMap
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.skull.connection.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.skull.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.skull.connection.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.skull.connection.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.skull.connection.packet.{Packet, PacketCoordinates}
import java.util.NoSuchElementException

import fr.`override`.linkit.core.connection.packet
import fr.`override`.linkit.core.connection.packet.traffic.channel

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
abstract class SharedCacheHandler(val family: String, traffic: PacketTraffic) extends Updatable {

    protected val communicator: packet.traffic.channel.CommunicationPacketChannel =
        traffic.getInjectable(11, ChannelScope.broadcast, channel.CommunicationPacketChannel.providable)

    private val sharedObjects: map.SharedMap[Long, Serializable] = init()
    println(s"sharedObjects = ${sharedObjects}")

    def post[A <: Serializable](key: Long, value: A): A = {
        sharedObjects.put(key, value)
        value
    }

    def get[A <: Serializable](key: Long): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]
    def getOrWait[A <: Serializable](key: Long): A = sharedObjects.getOrWait(key).asInstanceOf[A]
    def apply[A <: Serializable](key: Long): A = sharedObjects(key).asInstanceOf[A]

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

    override def update(): this.type = {
        LocalCacheHandler.updateAll()
        //sharedObjects will be updated by LocalCacheHandler.updateAll
        this
    }

    private def retrieveBaseContent(cacheID: Long): Array[Any] = {
        println(s"Sending request to server in order to retrieve content of cache number $cacheID")
        communicator.sendRequest(WrappedPacket(family, LongPacket(cacheID)), ServerIdentifier)
        println(s"request sent !")
        val content = communicator.nextResponse[ArrayObjectPacket].value //The request will return the cache content
        println(s"Content received ! (${content.mkString("Array(", ", ", ")")})")
        content.asInstanceOf[Array[Any]]
    }

    private def init(): map.SharedMap[Long, Serializable] = {
        if (this.sharedObjects != null)
            throw new IllegalStateException("This SharedCacheHandler is already initialised !")

        initPacketHandling()

        val content = retrieveBaseContent(1)

        val cacheOwners = map.SharedMap[Long, Serializable].createNew(this, 1, content, communicator)
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
        Console.println(s"<$family> $msg")
    }

}

object SharedCacheHandler {

    private val caches = mutable.HashMap.empty[(String, PacketTraffic), SharedCacheHandler]

    def get(identifier: String,
            factory: (String, PacketTraffic) => SharedCacheHandler = new SimpleSharedCacheHandler(_, _))
           (implicit traffic: PacketTraffic): SharedCacheHandler = {

        caches.get((identifier, traffic))
                .fold {
                    println(s"--> CREATING SHARED CACHE HANDLER <$identifier>")
                    val cache = factory(identifier, traffic)
                    println(s"--> SHARED CACHE HANDLER CREATED <$identifier>")
                    caches.put((identifier, traffic), cache)
                    cache
                }(cache => {
                    println(s"--> UPDATING CACHE <$identifier> INSTEAD OF CREATING IT.")
                    cache.update()
                    println(s"--> UPDATED CACHE <$identifier> INSTEAD OF CREATING IT.")
                    cache
                })
    }

    class SimpleSharedCacheHandler private[SharedCacheHandler](family: String, traffic: PacketTraffic)
            extends SharedCacheHandler(family, traffic) {

        override def continuePacketHandling(packet: Packet, coords: DedicatedPacketCoordinates): Unit = {
            throw new UnexpectedPacketException("Received forbidden/not handleable packet for this simple shared cache handler")
        }
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