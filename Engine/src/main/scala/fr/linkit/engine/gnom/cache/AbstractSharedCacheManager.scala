/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache._
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.ContentHandler
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.traffic.DefaultCachePacketChannel
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.request.SimpleRequestPacketChannel

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

abstract class AbstractSharedCacheManager(override val family: String,
                                          override val network: Network,
                                          store: PacketInjectableStore) extends SharedCacheManager {

    println(s"New SharedCacheManager created ! $family")
    protected val channel          : SimpleRequestPacketChannel  = store.getInjectable(family.hashCode - 5, SimpleRequestPacketChannel, ChannelScopes.discardCurrent)
    protected val broadcastScope   : ChannelScope                = prepareScope(ChannelScopes.broadcast)
    protected val currentIdentifier: String                      = network.connection.currentIdentifier
    override  val reference        : SharedCacheManagerReference = SharedCacheManagerReference(family)
    //override  val trafficPath      : Array[Int]                 = store.trafficPath

    postInit()

    override def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A], behavior: CacheSearchBehavior): A = {
        LocalCachesStore
                .findCache[A](cacheID)
                .getOrElse {
                    preCacheOpenChecks(cacheID, classTag[A].runtimeClass)
                    val channel     = store.getInjectable(cacheID, DefaultCachePacketChannel(cacheID, this), ChannelScopes.broadcast)
                    val sharedCache = factory.createNew(channel)
                    LocalCachesStore.store(cacheID, sharedCache, channel)

                    println(s"OPENING CACHE $cacheID OF TYPE ${classTag[A].runtimeClass}")
                    val baseContent = retrieveCacheContent(cacheID, behavior).orNull
                    println(s"CONTENT RECEIVED (${baseContent}) FOR CACHE $cacheID")

                    if (baseContent != null) {
                        channel.getHandler.foreach {
                            case e: ContentHandler[CacheContent] => e.initializeContent(baseContent)
                            case _                               => //Simply don't set the content
                        }
                    }
                    channel.injectStoredBundles()
                    sharedCache
                }
    }

    override def getCacheInStore[A <: SharedCache : ClassTag](cacheID: Int): A = {
        LocalCachesStore
                .findCache[A](cacheID)
                .getOrElse {
                    throw new NoSuchCacheException(s"No cache was found in the local cache manager for cache identifier $cacheID.")
                }
    }

    override def update(): this.type = {
        println(s"all Cache of family '$family' will be updated.")
        LocalCachesStore.updateAll()
        this
    }

    def handleRequest(requestBundle: RequestPacketBundle): Unit

    protected def preCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit

    protected def prepareScope(factory: ScopeFactory[_ <: ChannelScope]): ChannelScope = {
        val traffic = channel.traffic
        val writer  = traffic.newWriter(channel.trafficPath)
        val scope   = factory.apply(writer)
        scope.addDefaultAttribute("family", family)
        scope
    }

    protected object LocalCachesStore {

        case class RegisteredCache(cache: SharedCache, channel: CachePacketChannel) {

            def getContent: Option[CacheContent] = channel.getHandler.map {
                //TODO don't let the content be retrieved by any engine.
                case handler: ContentHandler[CacheContent] => handler.getContent
                case _                                     => null
            }
        }

        @transient private val localRegisteredHandlers = mutable.HashMap.empty[Int, RegisteredCache]

        def updateAll(): Unit = {
            println(s"updating cache ($localRegisteredHandlers)...")
            localRegisteredHandlers
                    .foreach(_._2.cache.update())
            println(s"cache updated ! ($localRegisteredHandlers)")
        }

        def store(identifier: Int, cache: SharedCache, channel: CachePacketChannel): Unit = {
            println(s"Registering $identifier into local cache.")
            localRegisteredHandlers.put(identifier, RegisteredCache(cache, channel))
            println(s"Local cache is now $localRegisteredHandlers")
        }

        def unregister(identifier: Int): Unit = {
            println(s"Removing cache $identifier")
            localRegisteredHandlers.remove(identifier)
            println(s"Cache is now $identifier")
        }

        def getContent(cacheID: Int): Option[CacheContent] = {
            getCache(cacheID).flatMap(_.getContent)
        }

        def getCache(cacheID: Int): Option[RegisteredCache] = {
            localRegisteredHandlers.get(cacheID)
        }

        def findCache[A: ClassTag](cacheID: Int): Option[A] = {
            val opt            = localRegisteredHandlers
                    .get(cacheID)
                    .map(_.cache)
                    .asInstanceOf[Option[A]]
            val requestedClass = classTag[A].runtimeClass
            opt match {
                case Some(c: SharedCache) if c.getClass != requestedClass => throw new CacheNotAcceptedException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. (${c.getClass} vs $requestedClass)")
                case other                                                => other
            }
        }

        override def toString: String = localRegisteredHandlers.toString()

    }

    protected def println(msg: => String): Unit = {
        AppLogger.trace(s"$currentTasksId <> <$family, $ownerID> $msg")
    }

    private def postInit(): Unit = {
        /*val refStore = network.rootRefStore
        val shift    = family.hashCode
        refStore putAllNotContained Map(
            shift + 1 -> channel,
            shift + 2 -> broadcastScope,
            shift + 3 -> store
        )*/
        channel.addRequestListener(handleRequest)
    }
}

object AbstractSharedCacheManager {

    //The identifiers of caches used for system. Creating a cache between MinSystemCacheID and MaxSystemCacheID is not recommended.
    val MinSystemCacheID: Int   = 0
    val MaxSystemCacheID: Int   = 50
    val SystemCacheRange: Range = MinSystemCacheID to MaxSystemCacheID

}