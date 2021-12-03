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

package fr.linkit.api.gnom.cache

import fr.linkit.api.gnom.network.{Network, Updatable}
import fr.linkit.api.gnom.reference.NetworkObject
import fr.linkit.api.gnom.reference.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.TrafficInterestedNPH

import scala.reflect.ClassTag

/**
 * This class is the main point of the shared cache feature.
 * It handles cache content synchronisation and cache registration between the engines of a network.
 * For example, you can synchronise a list between engine A and engine B, in which each modification will be send to the other engine
 * in order to have the same list with the same items in the same orders.
 * */
trait SharedCacheManager extends NetworkObject[SharedCacheManagerReference] with Updatable {

    /**
     * Each Cache manager have a family string, it's in fact the identifier of the cache manager.
     * For example, in the [[Network]], each [[fr.linkit.api.gnom.network.Engine]] have a SharedCacheManager instance
     * where the family string value is equals to the engine's identifier.
     * */
    val family : String
    /**
     * The [[fr.linkit.api.gnom.network.Engine]] identifier that owns this cache.
     * The engine owner controls who can attach to a [[SharedCache]] and who can create a [[SharedCache]]
     * */
    val ownerID: String
    /**
     * The network that hosts this cache
     * */
    val network: Network

    def getCachesLinker: InitialisableNetworkObjectLinker[SharedCacheReference] with TrafficInterestedNPH

    def attachToCache[A <: SharedCache : ClassTag](cacheID: Int)(implicit factory: SharedCacheFactory[A]): A = {
        attachToCache[A](cacheID, CacheSearchBehavior.GET_OR_OPEN)
    }

    /**
     * Retrieves a [[SharedCache]] hosted by this SharedCacheManager. <br>
     * This method will create and synchronise the current content of the cache that is placed on the cacheID :<br>
     * If no cache is opened on the cacheID, it will simply be created and have an empty content.<br>
     * If the cache was already opened by another engine, the cache instance will be created and its content will be synchronised.<br>
     * The cache will then be stored locally if it was not present in it.<br>
     * If the cache was already created and registered locally, the factory will not be called and the cached instance will
     * then be returned.<br>
     * Note: can't be sure that every cache instances with the same cacheID are of the same type.<br>
     *
     * @throws CacheNotAcceptedException if the cache is already created but the cache's does not equals to the given one.
     * @param cacheID the cache identifier
     * @param factory the factory that will create the cache instance
     * @param behavior the kind of behavior to adopt when creating a cache
     * @return the cache instance.
     * @see [[SharedCache]]
     * @see [[SharedCacheFactory]]
     * @see [[CacheSearchBehavior]]
     * */
    def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, behavior: CacheSearchBehavior)(implicit factory: SharedCacheFactory[A]): A = {
        attachToCache[A](cacheID, factory, behavior)
    }

    def attachToCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A], behavior: CacheSearchBehavior = CacheSearchBehavior.GET_OR_OPEN): A

    /**
     * Get cache that is already opened and registered in the local cache.
     * The cache content will not be retrieved, but if no cache is found in the local cache, this cache manager will
     * not try to retrieve it on the network.
     * @throws NoSuchCacheException if no cache was found locally.
     * @param cacheID the cache identifier
     * @return the cache instance.
     */
    def getCacheInStore[A <: SharedCache : ClassTag](cacheID: Int): A

    /**
     * Retrieves the cache content of a given cache identifier.
     *
     * @param cacheID the identifier of a cache content that needs to be retrieved.
     * @param behavior the kind of behavior to adopt when retrieving a cache content
     * @return Some(content) if the cache content was retrieved, None if no cache has been found.
     * @throws CacheOpenException if something went wrong during the cache content retrieval (can be affected by behavior parameter)
     * @see [[CacheContent]]
     * */
    def retrieveCacheContent(cacheID: Int, behavior: CacheSearchBehavior): Option[CacheContent]

    def setCacheChannelToPerformant(cacheID: Int): Unit

    def setCacheChannelToSequential(cacheID: Int): Unit

}
