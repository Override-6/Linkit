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

package fr.linkit.engine.connection.network

import fr.linkit.api.connection.cache.{CacheSearchBehavior, SharedCacheManager}
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.api.connection.packet.PacketBundle
import fr.linkit.api.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.NetworkSharedCacheManager
import fr.linkit.engine.connection.cache.collection.{BoundedCollection, SharedCollection}
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacketChannel
import sun.misc.Unsafe

import scala.collection.mutable

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    private   val cacheRequestChannel                          = connection.getInjectable(12, ChannelScopes.discardCurrent, RequestPacketChannel)
    private   val caches                                       = mutable.HashMap.empty[String, NetworkSharedCacheManager]
    override  val cache             : SharedCacheManager       = initCaches()
    protected val sharedIdentifiers : SharedCollection[String] = cache.attachToCache(3, SharedCollection.set[String], CacheSearchBehavior.GET_OR_WAIT)
    protected val entityCommunicator: SyncAsyncPacketChannel   = connection.getInjectable(9, ChannelScopes.discardCurrent, SyncAsyncPacketChannel.busy)
    protected val entities: BoundedCollection.Immutable[Engine]

    override def listEngines: List[Engine] = {
        entities.toList
    }

    override def isConnected(identifier: String): Boolean = getEngine(identifier).isDefined

    override def getEngine(identifier: String): Option[Engine] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    override def attachToCacheManager(family: String, owner: ConnectionContext): SharedCacheManager = {
        attachToCachesManager(family, owner.currentIdentifier)
    }

    protected def attachToCachesManager(family: String, owner: String): SharedCacheManager = {
        if (family == null || owner == null)
            throw new NullPointerException("Family or owner is null.")

        caches.getOrElse(family, {
            AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: --> CREATING NEW SHARED CACHE MANAGER <$family, $owner>")
            val cache = new NetworkSharedCacheManager(family, owner, this, connection, cacheRequestChannel)

            //Will inject all packet that the new cache have possibly missed.
            caches.synchronized {
                AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: PUTTING CACHE <$family, $owner> INTO CACHES")
                caches.put(family, cache)
                AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: CACHES <$family, $owner> IS NOW : $caches")
            }
            cacheRequestChannel.injectStoredBundles()
            cache: SharedCacheManager
        })
    }

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        caches.get(family)
    }

    override def newCacheManager(family: String, owner: ExternalConnection): SharedCacheManager = {
        attachToCachesManager(family, owner.boundIdentifier)
    }

    protected def createEngine(identifier: String, communicationChannel: SyncAsyncPacketChannel): Engine

    protected def createEntity(identifier: String): Engine = {
        if (identifier == connection.currentIdentifier) {
            return connectionEngine
        }

        val channel = entityCommunicator.subInjectable(Array(identifier), SyncAsyncPacketChannel.busy, transparent = true)
        val ent     = createEngine(identifier, channel)
        ent
    }

    private def initCaches(): SharedCacheManager = {
        connection.translator.initNetwork(this)

        def findCacheToNotify(bundle: PacketBundle)
                             (notifyAction: NetworkSharedCacheManager => Unit): Unit = {
            val attr = bundle.attributes
            attr.getAttribute[String]("family") match {
                case Some(family) =>
                    val opt = caches.synchronized {
                        AppLogger.vWarn(s"$currentTasksId <> ${connection.currentIdentifier}: FINDING CACHE '$family' FOR PACKET ${bundle.packet} into $caches")
                        caches.get(family)
                    }
                    opt
                            //If cache does not contains the family tag, this mean that it could possibly be
                            //opened in the future, so received packets will be stored and reInjected every
                            //time a cache opens.
                            .fold(bundle.store())(cache => {
                                notifyAction(cache)
                            })
            }
        }

        cacheRequestChannel.addRequestListener(bundle => {
            AppLogger.vDebug(s"Request body: ${bundle}")
            findCacheToNotify(bundle) {
                _.handleRequest(bundle)
            }
        })

        attachToCachesManager(s"Global Cache", serverIdentifier)
    }

}

