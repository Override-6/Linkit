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

package fr.linkit.api.connection.cache

import fr.linkit.api.connection.network.{Network, Updatable}
import fr.linkit.api.local.concurrency.{IllegalThreadException, workerExecution}

import scala.reflect.ClassTag

/**
 * This class is the main point of the shared cache feature.
 * It handles cache content synchronisation and cache registration between the engines of a network.
 * For example, you can synchronise a list between engine A and engine B, in which each modification will be send to the other engine
 * in order to have the same list with the same items in the same orders.
 * */
trait SharedCacheManager extends Updatable {

    /**
     * Each Cache manager have a family string, it's in fact the identifier of the cache manager.
     * For example, in the [[Network]], each [[fr.linkit.api.connection.network.Engine]] have a SharedCacheManager instance
     * where the family string value is equals to the engine's identifier.
     * */
    val family : String
    /**
     * The [[fr.linkit.api.connection.network.Engine]] identifier that owns this cache.
     * the value of [[fr.linkit.api.connection.network.Engine.cache]] have the same value for family and ownerID.
     * */
    val ownerID: String
    /**
     * The network that hosts this cache
     * */
    val network: Network

    /**
     * A quick way to place instances in a shared cache manager then retrieve it in another engine
     * with [[findInstance()]].
     *
     * @param key the instance key, used to retrieve the value
     * @param value the value to place in the quick cache map.
     * @return the value parameter.
     * */
    def postInstance[A <: Serializable](key: Int, value: A): value.type

    /**
     * retrieve an instance bounded to a key in the quick cache map
     * @param key the key
     * @return Some(value) if a value bound with the given key has been found on the cache, None instead.
     * @throws ClassCastException if the value is not an instance of A
     * */
    def findInstance[A <: Serializable](key: Int): Option[A]

    /**
     * Will work as [[findInstance()]] except that the current thread will be paused until a value is placed in the map
     * with the given key
     * @param key the key
     * @return Some(value) if a value bound with the given key has been found on the cache, None instead.
     * @throws ClassCastException if the value is not an instance of A
     * */
    def getInstanceOrWait[A <: Serializable](key: Int): A

    /**
     * Retrieve an instance bounded to a key in the quick cache map
     * @param key the key
     * @return the value bound with the given key
     * @throws ClassCastException if the value is not an instance of A
     * @throws NoSuchElementException if no value bound with the key was found in the quick cache map
     * */
    def apply[A <: Serializable](key: Int): A

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
     * @param cacheID the cache identifier
     * @param factory the factory that will create the cache instance
     * @param behavior the kind of behavior to adopt when creating a cache
     * @return the cache instance.
     * @see [[SharedCache]]
     * @see [[SharedCacheFactory]]
     * @see [[CacheSearchBehavior]]
     * */
    def retrieveCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A with InternalSharedCache], behavior: CacheSearchBehavior = CacheSearchBehavior.GET_OR_OPEN): A

    /**
     * Works same as [[retrieveCache]] except that the cache content is retrieved in another worker thread task.
     * @throws IllegalThreadException if the current thread is not a [[fr.linkit.api.local.concurrency.WorkerThread]]
     * @param cacheID the cache identifier
     * @param factory the factory that will create the cache instance
     * @param behavior the kind of behavior to adopt when creating a cache
     * @return the cache instance.
     * @see [[fr.linkit.api.local.concurrency.WorkerPool]]
     * @see [[fr.linkit.api.local.concurrency.WorkerThread]]
     * */
    @workerExecution
    def retrieveCacheAsync[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A with InternalSharedCache], behavior: CacheSearchBehavior = CacheSearchBehavior.GET_OR_OPEN): A

    /**
     * Get cache that is already opened and registered in the local cache.
     * The cache content will not be retrieved, but if no cache is found in the local cache, this cache manager will
     * not try to retrieve it on the network.
     * @throws NoSuchCacheException if no cache was found locally.
     * @param cacheID the cache identifier
     * @return the cache instance.
     */
    def getCache[A <: SharedCache : ClassTag](cacheID: Int): A

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

}
