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
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, TrafficInterestedNPH}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker}
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.traffic.DefaultCachePacketChannel
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.request.SimpleRequestPacketChannel
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

abstract class AbstractSharedCacheManager(override val family: String,
                                          override val network: Network,
                                          store: PacketInjectableStore) extends SharedCacheManager {

    println(s"New SharedCacheManager created ! $family")
    protected val channel          : SimpleRequestPacketChannel  = store.getInjectable(family.hashCode - 5, SimpleRequestPacketChannel, ChannelScopes.discardCurrent)
    protected val broadcastScope   : ChannelScope                = prepareScope(ChannelScopes.broadcast)
    protected val currentIdentifier: String                      = network.connection.currentIdentifier
    override  val reference        : SharedCacheManagerReference = new SharedCacheManagerReference(family)
    override  val trafficPath      : Array[Int]                  = store.trafficPath

    postInit()

    override def getCachesLinker: NetworkObjectLinker[SharedCacheReference] with TrafficInterestedNPH = ManagerCachesLinker

    override def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A], behavior: CacheSearchBehavior): A = {
        LocalCachesStore
            .findCacheSecure[A](cacheID)
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
            .findCacheSecure[A](cacheID)
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

            val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH] = {
                channel.getHandler.flatMap {
                    case handler: ContentHandler[_] => handler.objectLinker
                    case _                          => None
                }
            }

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
            ManagerCachesLinker.registerReference(cache.reference)
            println(s"Local cache is now $localRegisteredHandlers")
        }

        def unregister(identifier: Int): Unit = {
            println(s"Removing cache $identifier")
            localRegisteredHandlers.remove(identifier).fold()(profile => {
                ManagerCachesLinker.unregisterReference(profile.cache.reference)
            })
            println(s"Cache is now $identifier")
        }

        def getContent(cacheID: Int): Option[CacheContent] = {
            findCache(cacheID).flatMap(_.getContent)
        }

        def findCache(cacheID: Int): Option[RegisteredCache] = {
            localRegisteredHandlers.get(cacheID)
        }

        def findCacheSecure[A: ClassTag](cacheID: Int): Option[A] = {
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

    protected object ManagerCachesLinker
        extends AbstractNetworkPresenceHandler[SharedCacheReference](network.objectManagementChannel)
            with NetworkObjectLinker[SharedCacheReference] with TrafficInterestedNPH {


        override def registerReference(ref: SharedCacheReference): Unit = super.registerReference(ref)

        override def unregisterReference(ref: SharedCacheReference): Unit = super.unregisterReference(ref)

        override def findPresence(reference: SharedCacheReference): Option[NetworkObjectPresence] = {
            if (reference.getClass == classOf[SharedCacheReference])
                super.findPresence(reference)
            else {
                LocalCachesStore.findCache(reference.cacheID)
                    .flatMap(_.objectLinker.flatMap(_.findPresence(silentCast(reference))))
            }
        }

        override def findObject(reference: SharedCacheReference): Option[NetworkObject[_ <: SharedCacheReference]] = {
            val cacheOpt = LocalCachesStore.findCache(reference.cacheID)
            if (reference.getClass == classOf[SharedCacheReference])
                cacheOpt.map(_.cache)
            else
                cacheOpt.flatMap(_.objectLinker.flatMap(_.findObject(silentCast(reference))))
        }

        private def silentCast[X](t: AnyRef): X = t.asInstanceOf[X]

        override def injectRequest(bundle: LinkerRequestBundle): Unit = {
            val reference = bundle.linkerReference
            if (reference.getClass eq classOf[SharedCacheReference])
                handleBundle(bundle)
            else {
                reference match {
                    case ref: SharedCacheReference =>
                        LocalCachesStore.findCache(ref.cacheID).fold() { cacheProfile =>
                            cacheProfile.objectLinker.fold()(_.injectRequest(bundle))
                        }
                }
            }
        }
    }

    protected def println(msg: => String): Unit = {
        AppLogger.trace(s"$currentTasksId <> <$family, $ownerID> $msg")
    }

    private def postInit(): Unit = {
        channel.addRequestListener(handleRequest)
    }
}

object AbstractSharedCacheManager {

    //The identifiers of caches used for system. Creating a cache between MinSystemCacheID and MaxSystemCacheID is not recommended.
    val MinSystemCacheID: Int   = 0
    val MaxSystemCacheID: Int   = 50
    val SystemCacheRange: Range = MinSystemCacheID to MaxSystemCacheID

}