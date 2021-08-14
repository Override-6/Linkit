/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache

import fr.linkit.api.connection.cache._
import fr.linkit.api.connection.cache.traffic.handler.ContentHandler
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.NetworkSharedCacheManager.MockCache
import fr.linkit.engine.connection.cache.traffic.DefaultCachePacketChannel
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestPacketBundle, RequestPacketChannel}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class NetworkSharedCacheManager(override val family: String,
                                override val ownerID: String,
                                override val network: Network,
                                requestChannel: RequestPacketChannel) extends SharedCacheManager {

    private val currentIdentifier = network.connection.currentIdentifier
    private val isManagingSelf    = ownerID == currentIdentifier
    private val ownerScope        = prepareScope(ChannelScopes.include(ownerID))
    private val broadcastScope    = prepareScope(ChannelScopes.broadcast)
    /*private lazy val quickCache: map.SharedMap[Int, Serializable] = init()

    override def postInstance[A <: Serializable](key: Int, value: A): value.type = {
        quickCache.put(key, value)
        value
    }

    override def findInstance[A <: Serializable](key: Int): Option[A] = {
        quickCache.get(key).asInstanceOf[Option[A]]
    }

    override def getInstanceOrWait[A <: Serializable](key: Int): A = {
        val obj = quickCache.getOrWait(key)
        obj.asInstanceOf[A]
    }

    override def apply[A <: Serializable](key: Int): A = quickCache(key).asInstanceOf[A]*/

    override def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, behavior: CacheSearchBehavior)(implicit factory: SharedCacheFactory[A]): A = {
        LocalCacheManager
                .findCache[A](cacheID)
                .getOrElse {
                    val channel     = new DefaultCachePacketChannel(broadcastScope, this, cacheID)
                    val sharedCache = factory.createNew(channel)
                    LocalCacheManager.register(cacheID, sharedCache)

                    println(s"OPENING CACHE $cacheID OF TYPE ${classTag[A].runtimeClass}")
                    val baseContent = retrieveCacheContent(cacheID, behavior).orNull
                    println(s"CONTENT RECEIVED (${baseContent}) FOR CACHE $cacheID")

                    if (baseContent != null) {
                        channel.getHandler.foreach {
                            case e: ContentHandler[CacheContent] => e.setContent(baseContent)
                            case _                               => //Simply don't set the content
                        }
                    }
                    requestChannel.injectStoredBundles()
                    sharedCache
                }
    }

    override def getCacheInLocal[A <: SharedCache : ClassTag](cacheID: Int): A = {
        LocalCacheManager
                .findCache[A](cacheID)
                .getOrElse {
                    throw new NoSuchCacheException(s"No cache was found in the local cache manager for cache identifier $cacheID.")
                }
    }

    override def retrieveCacheContent(cacheID: Int, behavior: CacheSearchBehavior): Option[CacheContent] = {
        println(s"Sending request to $ownerID in order to retrieve content of cache number $cacheID")
        val request = requestChannel
                .makeRequest(ownerScope)
                .putAttribute("behavior", behavior)
                .addPacket(IntPacket(cacheID))
                .submit()

        try {
            val response         = request.nextResponse
            val possibleErrorMsg = response.getAttribute[String]("errorMsg")

            if (possibleErrorMsg.isDefined) {
                val errorMsg = possibleErrorMsg.get
                throw new CacheOpenException(errorMsg + s"(this = $ownerID)")
            }

            val content = response.nextPacket[RefPacket[Option[CacheContent]]].value
            request.detach()

            println(s"Content '$cacheID' received ! ($content)")
            content
        } catch {
            case e: Throwable =>
                AppLogger.fatal(s"Was executing request (${request.id}) for cache ID '$cacheID'.")
                AppLogger.printStackTrace(e)
                System.exit(1)
                throw null
        }
    }

    override def update(): this.type = {
        println(s"all Cache of family '$family' will be updated.")
        LocalCacheManager.updateAll()
        this
    }

    def forget(cacheID: Int): Unit = {
        LocalCacheManager.unregister(cacheID)
    }

    def handleRequest(requestBundle: RequestPacketBundle): Unit = {
        val coords   = requestBundle.coords
        val request  = requestBundle.packet
        val response = requestBundle.responseSubmitter
        println(s"HANDLING REQUEST $request, $coords")

        if (!isManagingSelf) {
            val msg =
                s"""This request can't be processed by this engine.
                   | The request must be performed to the engine that hosts the manager.
                   | (Current Identifier = $currentIdentifier, host identifier = $ownerID)""".stripMargin
            response.putAttribute("errorMsg", msg)
            response.submit()
            return
        }

        handleContentRetrieval(requestBundle)
    }

    private def handleContentRetrieval(requestBundle: RequestPacketBundle): Unit = {
        val coords   = requestBundle.coords
        val request  = requestBundle.packet
        val response = requestBundle.responseSubmitter

        val senderID: String = coords.senderID
        val behavior         = request.getAttribute[CacheSearchBehavior]("behavior").get //throw
        val cacheID          = request.nextPacket[IntPacket].value
        println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
        println(s"Behavior = $behavior")

        val contentOpt = LocalCacheManager.getContent(cacheID)
        if (contentOpt.isDefined) {
            response.addPacket(RefPacket[Option[CacheContent]](contentOpt))
        } else {
            import CacheSearchBehavior._

            behavior match {
                case GET_OR_CRASH =>
                    val msg = s"Requested cache of identifier '$cacheID' is not opened or isn't handled by this connection."
                    response.putAttribute("errorMsg", msg)
                case GET_OR_WAIT  =>
                    //If the requester is not the owner, wait the owner to open the cache.
                    if (senderID != ownerID) {
                        requestChannel.storeBundle(requestBundle)
                        println(s"Await open ($cacheID)...")
                        return
                    }
                    //The sender is the owner : this class must create the cache content.
                    response.addPacket(RefPacket[Option[CacheContent]](None))
                case GET_OR_OPEN  =>
                    response.addPacket(RefPacket[Option[CacheContent]](None))
            }
        }
        println(s"Content Response for cache $cacheID = $response.")
        response.submit()
    }

    /*
        private def init(): Unit = {
            /*
            * Don't touch, scala companions instances works as a lazy val, and all lazy val are synchronized on the instance that
            * they are computing. If you remove this line, NetworkSCManager could some times be deadlocked because retrieveCacheContent
            * will wait for its content's request, and thus its request response will be handled by another thread,
            * which will need LocalCacheHandler in order to retrieve the local cache, which is synchronized, so it will
            * be blocked until the thread that requested the content get it's response, but it's impossible because the thread
            * that handles the request is waiting...
            * For simple, if you remove this line, a deadlock will occur.
            * */
            LocalCacheManager.loadClass()
            /*val content       = retrieveCacheContent(1, CacheSearchBehavior.GET_OR_WAIT)
            val sharedObjects = SharedMap[Int, Serializable].createNew(this, 1, container)
            if (content.isDefined) {
                sharedObjects.setContent(content.get)
            }

            LocalCacheManager.register(1, sharedObjects)
            sharedObjects.foreachKeys(LocalCacheManager.registerMock) //mock all current caches that are registered on this family
            println("Shared objects : " + sharedObjects)
            sharedObjects*/
        }
    */
    private def prepareScope(factory: ScopeFactory[_ <: ChannelScope]): ChannelScope = {
        val traffic = requestChannel.traffic
        val writer  = traffic.newWriter(requestChannel.identifier)
        val scope   = factory.apply(writer)
        scope.addDefaultAttribute("family", family)
        scope
    }

    protected object LocalCacheManager {

        private val localRegisteredCaches = mutable.Map.empty[Int, SharedCache]

        def updateAll(): Unit = {
            println(s"updating cache ($localRegisteredCaches)...")
            localRegisteredCaches
                    .foreach(_._2.update())
            println(s"cache updated ! ($localRegisteredCaches)")
        }

        def register(identifier: Int, cache: SharedCache): Unit = {
            println(s"Registering $identifier into local cache.")
            localRegisteredCaches.put(identifier, cache)
            println(s"Local cache is now $localRegisteredCaches")
        }

        def unregister(identifier: Int): Unit = {
            println(s"Removing cache $identifier")
            localRegisteredCaches.remove(identifier)
            println(s"Cache is now $identifier")
        }

        def registerMock(identifier: Int, cacheType: Class[_]): Unit = {
            localRegisteredCaches.put(identifier, new MockCache(cacheType))
        }

        def getContent(cacheID: Int): Option[CacheContent] = {
            localRegisteredCaches.get(cacheID).map(_.snapshotContent)
        }

        def isRegistered(cacheID: Int): Boolean = {
            localRegisteredCaches.contains(cacheID)
        }

        def findCache[A: ClassTag](cacheID: Int): Option[A] = {
            val opt            = localRegisteredCaches.get(cacheID).asInstanceOf[Option[A]]
            val requestedClass = classTag[A].runtimeClass
            opt match {
                case Some(_: MockCache)                                   => None
                case Some(c: SharedCache) if c.getClass != requestedClass => throw new CacheTypeMismatchException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. (${c.getClass} vs $requestedClass)")
                case other                                                => other
            }
        }

        override def toString: String = localRegisteredCaches.toString()

        /*
        * Don't touch, scala companions instances works as a lazy val, and all lazy val are synchronized on the instance that
        * they are computing. If you remove this line, NetworkSCManager could some times be deadlocked because retrieveCacheContent
        * will wait for its content's request, and thus its request response will be handled by another thread,
        * which will need LocalCacheHandler in order to retrieve the local cache, which is synchronized, so it will
        * be blocked until the thread that requested the content get it's response, but it's impossible because the thread
        * that handles the request is waiting...
        * For simple, if you remove this method, a deadlock will occur.
        * */
        def loadClass(): Unit = ()

    }

    private def println(msg: => String): Unit = {
        AppLogger.trace(s"$currentTasksId <> <$family, $ownerID> $msg")
    }

}

object NetworkSharedCacheManager {

    class MockCache(val cacheType: Class[_]) extends SharedCache {

        override val cacheID: Int = -1

        override val family: String = ""

        override def snapshotContent: CacheContent = CacheArrayContent(Array())

        //override def setContent(cacheContent: CacheContent): Unit = ()

        override def update(): this.type = this
    }

}