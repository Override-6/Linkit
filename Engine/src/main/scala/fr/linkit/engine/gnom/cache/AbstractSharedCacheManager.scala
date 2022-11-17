/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache._
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.CacheContentHandler
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{Current, Everyone}
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.referencing.NetworkObject
import fr.linkit.api.gnom.referencing.linker.{InitialisableNetworkObjectLinker, NetworkObjectLinker}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.api.gnom.referencing.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.request.SimpleRequestPacketChannel
import fr.linkit.engine.gnom.referencing.NOLUtils.throwUnknownObject
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler
import fr.linkit.engine.internal.debug.{CacheOpenStep, Debugger}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import fr.linkit.api.gnom.network.tag.TagSelection._

abstract class AbstractSharedCacheManager(override val family : String,
                                          override val network: Network,
                                          omc                 : ObjectManagementChannel,
                                          store               : PacketInjectableStore) extends SharedCacheManager {

    protected val channel          : SimpleRequestPacketChannel  = store.getInjectable(family.hashCode - 5, SimpleRequestPacketChannel, ChannelScopes(!Current))
    protected val currentIdentifier: String                      = network.connection.currentName
    override  val reference        : SharedCacheManagerReference = new SharedCacheManagerReference(family)

    channel.addRequestListener(handleRequest)

    override def getCacheTrafficNode(cacheID: Int): TrafficNode[TrafficObject[TrafficReference]] = {
        LocalCachesStore.findCache(cacheID) match {
            case None                  => throw new NoSuchElementException(s"Could not find cache '$cacheID'")
            case Some(registeredCache) =>
                val channel = registeredCache.channel
                val path    = channel.trafficPath
                network.connection.traffic.findNode(path) match {
                    case None       => throw new NoSuchElementException(s"Could not find traffic node of channel ${channel.reference}, used by cache ${registeredCache.cache.reference} into traffic.")
                    case Some(node) =>
                        node
                }
        }
    }

    override def getCachesLinker: InitialisableNetworkObjectLinker[SharedCacheReference] with TrafficInterestedNPH = ManagerCachesLinker

    override def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A], method: CacheSearchMethod): A = /*this.synchronized*/ {
        AppLoggers.GNOM.debug(s"Attaching to cache $cacheID in $family. (method=$method, expected cache type: ${classTag[A].runtimeClass.getName})")
        LocalCachesStore
                .findCacheSecure[A](cacheID)
                .getOrElse(createCache(cacheID, factory, method))
    }

    override def getCacheInStore[A <: SharedCache : ClassTag](cacheID: Int): A = {
        LocalCachesStore
                .findCacheSecure[A](cacheID)
                .getOrElse {
                    throw new NoSuchCacheException(s"No cache was found in the local cache manager for cache identifier $cacheID.")
                }
    }

    protected def handleRequest(requestBundle: RequestPacketBundle): Unit

    protected def remoteCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit

    /**
     * Retrieves the cache content of a given cache identifier.
     *
     * @param cacheID  the identifier of a cache content that needs to be retrieved.
     * @param behavior the kind of behavior to adopt when retrieving a cache content
     * @return Some(content) if the cache content was retrieved, None if no cache has been found.
     * @throws CacheOpenException if something went wrong during the cache content retrieval (can be affected by behavior parameter)
     * @see [[CacheContent]]
     * */
    protected def retrieveCacheContent(cacheID: Int, behavior: CacheSearchMethod): Option[CacheContent]

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
                    case handler: CacheContentHandler[_] => handler.objectLinker
                    case _                               => None
                }
            }

            def getContent: Option[CacheContent] = channel.getHandler.map {
                //TODO don't let the content be retrievable by any engine.
                case handler: CacheContentHandler[_] => handler.getInitialContent
                case _                               => null
            }
        }

        @transient private val localRegisteredHandlers = mutable.HashMap.empty[Int, RegisteredCache]

        def store(identifier: Int, cache: SharedCache, channel: CachePacketChannel): Unit = {
            localRegisteredHandlers.put(identifier, RegisteredCache(cache, channel))
            ManagerCachesLinker.registerReference(cache.reference)
        }

        def unregister(identifier: Int): Unit = {
            localRegisteredHandlers.remove(identifier).fold()(profile => {
                ManagerCachesLinker.unregisterReference(profile.cache.reference)
            })
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
                case Some(c: SharedCache) if !requestedClass.isAssignableFrom(c.getClass) =>
                    throw new CacheNotAcceptedException(s"Attempted to open a cache of type '$cacheID' while a cache with the same id is already registered, but does not have the same type. (${c.getClass} vs $requestedClass)")
                case other                                                                => other
            }
        }

        override def toString: String = localRegisteredHandlers.toString()
    }

    private def createCache[A <: SharedCache](cacheID: Int, factory: SharedCacheFactory[A], method: CacheSearchMethod): A = try {
        Debugger.push(CacheOpenStep(reference / cacheID, factory.targetClass))
        AppLoggers.GNOM.debug(s"Creating cache $cacheID in $family.")
        remoteCacheOpenChecks(cacheID, factory.targetClass)
        val channel = store.getInjectable(cacheID, DefaultCachePacketChannel(cacheID, this), ChannelScopes(Everyone))
        chainWithTrunkIPU(channel)
        val sharedCache = factory.createNew(channel)
        LocalCachesStore.store(cacheID, sharedCache, channel)

        //println(s"OPENING CACHE $cacheID OF TYPE ${classTag[A].runtimeClass}")
        val baseContent = retrieveCacheContent(cacheID, method).orNull
        //println(s"CONTENT RECEIVED ($baseContent) FOR CACHE $cacheID")

        if (baseContent != null) {
            channel.getHandler.foreach {
                case e: CacheContentHandler[CacheContent] => e.initializeContent(baseContent)
                case _                                    => //if no content handler set, simply don't set the content
            }
        }
        channel.injectStoredBundles()
        sharedCache
    } finally Debugger.pop()

    private def chainWithTrunkIPU(trafficNode: InjectableTrafficNode[PacketInjectable]): Unit = {
        val global = network.globalCaches
        if ((global ne this) && !(trafficNode.injectable.trafficPath sameElements Array(0))) {
            val cacheTrunkNode = network.globalCaches.getCacheTrafficNode(0)
            trafficNode.chainIPU(cacheTrunkNode) // chain current with the Network trunk's cache synchronization channel
        }
    }


    protected object ManagerCachesLinker
            extends AbstractNetworkPresenceHandler[SharedCacheReference](network.gnol.cacheNOL, omc)
                    with InitialisableNetworkObjectLinker[SharedCacheReference] with TrafficInterestedNPH {

        override def registerReference(ref: SharedCacheReference): Unit = super.registerReference(ref)

        override def unregisterReference(ref: SharedCacheReference): Unit = super.unregisterReference(ref)

        override def getPresence(reference: SharedCacheReference): NetworkObjectPresence = {
            if (reference.getClass eq classOf[SharedCacheReference])
                super.getPresence(reference)
            else {
                LocalCachesStore.findCache(reference.cacheID)
                        .fold(super.getPresence(reference))(_.objectLinker.fold(super.getPresence(reference))(_.getPresence(cast(reference))))
            }
        }

        override def findObject(reference: SharedCacheReference): Option[NetworkObject[_ <: SharedCacheReference]] = {
            val cacheOpt = LocalCachesStore.findCache(reference.cacheID)
            if (reference.getClass eq classOf[SharedCacheReference])
                cacheOpt.map(_.cache)
            else
                cacheOpt.flatMap(_.objectLinker.flatMap(_.findObject(cast(reference))))
        }

        private def cast[X](t: AnyRef): X = t.asInstanceOf[X]

        override def injectRequest(bundle: LinkerRequestBundle): Unit = {
            val reference = bundle.linkerReference
            if (reference.getClass eq classOf[SharedCacheReference])
                super.injectRequest(bundle)
            else {
                reference match {
                    case ref: SharedCacheReference =>
                        LocalCachesStore.findCache(ref.cacheID).fold(throw new NoSuchElementException(s"Could not inject Linker request bundle: no such cache is set at $ref")) { cacheProfile =>
                            cacheProfile.objectLinker.fold(throw new UnsupportedOperationException(s"Could not inject Linker request bundle: cache located at '$ref' does not have an object linker."))(
                                _.injectRequest(bundle))
                        }
                }
            }
        }

        override def initializeObject(obj: NetworkObject[_ <: SharedCacheReference]): Unit = {
            val ref     = obj.reference
            val cacheID = ref.cacheID
            if (ref.getClass eq classOf[SharedCacheReference]) {
                if (LocalCachesStore.findCache(cacheID).isDefined)
                    throw new CacheNotAcceptedException(s"Could not initialize shared cache at $ref: a cache is already opened here.")
                obj match {
                    case cache: SharedCache =>
                        val channel = store.findInjectable[CachePacketChannel](cacheID).getOrElse {
                            throw new CacheNotAcceptedException(s"Could not initialize shared cache at $ref: no matching channel found for channel.")
                        }
                        LocalCachesStore.store(cacheID, cache, channel)
                    case _                  => throwUnknownObject(obj)
                }
                return
            }
            val profile = LocalCachesStore.findCache(cacheID).getOrElse {
                throw new NoSuchCacheException(s"Could not initialize cache item located at $ref: No such cache of id '$cacheID'.")
            }
            profile.objectLinker.getOrElse {
                throw new NoSuchElementException(s"Could not initialize cache item located at $ref: No Such Object linker for cache ${profile.cache.getClass.getName}")
            } match {
                case l: InitialisableNetworkObjectLinker[SharedCacheReference] => l.initializeObject(obj)
                case _                                                         => throw new UnsupportedOperationException(s"Could not initialize cache item located at $ref: current shared cache can't handle object initialization.")
            }
        }

    }
}

object AbstractSharedCacheManager {

    //The identifiers of caches used for system. Creating a cache between MinSystemCacheID and MaxSystemCacheID is not recommended.
    val MinSystemCacheID: Int   = 0
    val MaxSystemCacheID: Int   = 50
    val SystemCacheRange: Range = MinSystemCacheID to MaxSystemCacheID

}