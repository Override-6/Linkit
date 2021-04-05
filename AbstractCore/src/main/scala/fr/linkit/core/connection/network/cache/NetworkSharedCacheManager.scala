/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.network.cache

import fr.linkit.api.connection.network.cache.{CacheOpenBehavior, InternalSharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.traffic.ChannelScope
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.NetworkSharedCacheManager.MockCache
import fr.linkit.core.connection.network.cache.map.SharedMap
import fr.linkit.core.connection.packet.AbstractAttributePresence
import fr.linkit.core.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.linkit.core.connection.packet.fundamental.ValPacket.LongPacket
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestPacket, RequestPacketChannel, ResponseSubmitter}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

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
class NetworkSharedCacheManager(override val family: String,
                                override val ownerID: String,
                                cacheChannel: AsyncSenderSyncReceiver,
                                requestChannel: RequestPacketChannel) extends AbstractAttributePresence with SharedCacheManager {

    private      val ownerScope                                       = prepareOwnerScope()
    private lazy val sharedObjects: map.SharedMap[Long, Serializable] = init()

    override def postInstance[A <: Serializable](key: Long, value: A): A = {
        sharedObjects.put(key, value)
        value
    }

    override def getInstance[A <: Serializable](key: Long): Option[A] = sharedObjects.get(key).asInstanceOf[Option[A]]

    override def getInstanceOrWait[A <: Serializable](key: Long): A = sharedObjects.getOrWait(key).asInstanceOf[A]

    override def apply[A <: Serializable](key: Long): A = sharedObjects(key).asInstanceOf[A]

    override def getCache[A <: InternalSharedCache : ClassTag](cacheID: Long, factory: SharedCacheFactory[A], behavior: CacheOpenBehavior): A = {
        LocalCacheHandler
                .findCache[A](cacheID)
                .getOrElse {
                    println(s"OPENING CACHE $cacheID OF TYPE ${classTag[A].runtimeClass}")
                    val baseContent = retrieveCacheContent(cacheID, behavior)
                    println(s"CONTENT RECEIVED (${baseContent.mkString("Array(", ", ", ")")}) FOR CACHE $cacheID")
                    val sharedCache = factory.createNew(this, cacheID, baseContent, cacheChannel)
                    LocalCacheHandler.register(cacheID, sharedCache)
                    requestChannel.injectStoredSubmitters()
                    sharedCache
                }
    }

    override def getUpdated[A <: InternalSharedCache : ClassTag](cacheID: Long, factory: SharedCacheFactory[A], behavior: CacheOpenBehavior): A = {
        getCache(cacheID, factory, behavior).update()
    }

    override def retrieveCacheContent(cacheID: Long, behavior: CacheOpenBehavior): Array[Any] = {
        println(s"Sending request to $ownerID in order to retrieve content of cache number $cacheID")
        val request = requestChannel
                .makeRequest(ownerScope)
                .putAttribute("behavior", behavior)
                .addPacket(LongPacket(cacheID))
                .submit()

        try {
            val response         = request.nextResponse
            val possibleErrorMsg = response.getAttribute[String]("errorMsg")

            if (possibleErrorMsg.isDefined) {
                val errorMsg = possibleErrorMsg.get
                throw new CacheOpenException(errorMsg)
            }

            val content = response.nextPacket[ArrayObjectPacket].value
            request.delete()

            println(s"Content '$cacheID' received ! (${content.mkString("Array(", ", ", ")")})")
            content
        } catch {
            case e: Throwable =>
                AppLogger.fatal(s"Was executing request (${request.id}) for cache ID '$cacheID'.")
                AppLogger.printStackTrace(e)
                System.exit(1)
                throw new Error
        }
    }

    override def update(): this.type = {
        println("Cache will be updated.")
        LocalCacheHandler.updateAll()
        //sharedObjects will be updated by LocalCacheHandler.updateAll call
        this
    }

    override def getID: Int = family.hashCode

    def forget(cacheID: Long): Unit = {
        LocalCacheHandler.unregister(cacheID)
    }

    def handleCachePacket(packet: Packet, attributes: PacketAttributes, coords: DedicatedPacketCoordinates): Unit = {
        packet match {
            case WrappedPacket(key, subPacket) =>
                LocalCacheHandler.injectPacket(key.toLong, subPacket, attributes, coords)
        }
    }

    def handleRequest(packet: RequestPacket, coords: DedicatedPacketCoordinates, response: ResponseSubmitter): Unit = {
        println(s"HANDLING REQUEST $packet, $coords")

        val senderID: String = coords.senderID
        val behavior         = packet.getAttribute[CacheOpenBehavior]("behavior").get
        val cacheID          = packet.nextPacket[LongPacket].value

        println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
        val contentOpt = LocalCacheHandler.getContent(cacheID)

        if (contentOpt.isDefined) {
            response.addPacket(ArrayObjectPacket(contentOpt.get))
        } else {
            import CacheOpenBehavior._
            behavior match {
                case GET_OR_EMPTY => response.addPacket(ArrayObjectPacket())
                case GET_OR_CRASH => response.putAttribute("errorMsg", s"Requested cache of identifier '$cacheID' is not opened or isn't handled by this connection.")
                case AWAIT_OPEN =>
                    requestChannel.storeSubmitter(packet, coords, response)
                    println(s"Await open ($cacheID)...")
                    return
            }
        }
        val content = contentOpt.get

        println(s"Content = ${content.mkString("Array(", ", ", ")")}")

        response
                .addPacket(ArrayObjectPacket(content))
                .submit()
    }

    private def init(): SharedMap[Long, Serializable] = {
        /*
        * Don't touch, scala objects works as a lazy val, and all lazy val are synchronized on the instance that
        * they are computing. If you remove this line, NetworkSCManager could some times be deadlocked because retrieveCacheContent
        * will wait for its content's request, and thus its request response will be handled by another thread,
        * which will need LocalCacheHandler in order to retrieve the local cache, which is synchronized, so it will
        * be blocked until the thread that requested the content get it's response, but it's impossible because the thread
        * that handles the request is locking...
        * For simple, if you remove this line, a deadlock will occur.
        * */
        LocalCacheHandler
        val content = retrieveCacheContent(1, CacheOpenBehavior.AWAIT_OPEN)

        val sharedObjects = SharedMap[Long, Serializable].createNew(this, 1, content, cacheChannel)
        LocalCacheHandler.register(1L, sharedObjects)
        sharedObjects.foreachKeys(LocalCacheHandler.registerMock) //mock all current caches that are registered on this family
        println("Shared objects : " + sharedObjects)
        sharedObjects
    }

    private def prepareOwnerScope(): ChannelScope = {
        val writer = cacheChannel.traffic.newWriter(requestChannel.identifier)
        val scope  = ChannelScopes.reserved(ownerID)(writer)
        scope.addDefaultPresence(requestChannel, family)
        scope
    }

    protected object LocalCacheHandler {

        private val localRegisteredCaches = mutable.Map.empty[Long, InternalSharedCache]

        def updateAll(): Unit = {
            println(s"updating cache ($localRegisteredCaches)...")
            localRegisteredCaches
                    .foreach(_._2.update())
            println(s"cache updated ! ($localRegisteredCaches)")
        }

        def register(identifier: Long, cache: InternalSharedCache): Unit = {
            println(s"Registering $identifier into local cache.")
            localRegisteredCaches.put(identifier, cache)
            println(s"Local cache is now $localRegisteredCaches")
        }

        def unregister(identifier: Long): Unit = {
            println(s"Removing cache $identifier")
            localRegisteredCaches.remove(identifier)
            println(s"Cache is now $identifier")
        }

        def injectPacket(cacheID: Long, packet: Packet, attributes: PacketAttributes, coords: PacketCoordinates): Unit = try {
            localRegisteredCaches(cacheID).handlePacket(packet, attributes, coords)
        } catch {
            case _: NoSuchElementException =>
                println(s"Mocked $cacheID")
                registerMock(cacheID)
            case NonFatal(e)               => e.printStackTrace(Console.out)
        }

        def registerMock(identifier: Long): Unit = {
            localRegisteredCaches.put(identifier, MockCache)
        }

        def getContent(cacheID: Long): Option[Array[Any]] = {
            localRegisteredCaches.get(cacheID).map(_.currentContent)
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
                val presentClass   = opt.get.getClass
                throw new IllegalArgumentException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. ($presentClass vs $requestedClass)")
            }

            opt
        }

        override def toString: String = localRegisteredCaches.toString()

    }

    private def println(msg: String): Unit = {
        AppLogger.trace(s"$currentTasksId <> <$family, $ownerID> $msg")
    }

}

object NetworkSharedCacheManager {

    object MockCache extends AbstractSharedCache[Nothing](null, -1, null) {

        override val family: String = ""

        override var autoFlush: Boolean = false

        override def handlePacket(packet: Packet, attributes: PacketAttributes, coords: PacketCoordinates): Unit = ()

        override def currentContent: Array[Any] = Array()

        override def flush(): this.type = this

        override def modificationCount(): Int = -1

        override def update(): this.type = this

        override protected def setCurrentContent(content: Array[Nothing]): Unit = ()
    }

}