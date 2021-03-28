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

package fr.linkit.core.connection.network

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.network.{Network, NetworkEntity}
import fr.linkit.api.connection.packet.traffic.ChannelScope
import fr.linkit.api.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.core.connection.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.linkit.core.connection.network.cache.{SyncAsyncSender, SimpleSharedCacheManager}
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.core.local.system.AppLogger

import scala.collection.mutable

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    private   val cacheCommunicator                             = connection.getInjectable(11, ChannelScope.broadcast, new SyncAsyncSender(_))
    private   val caches                                        = mutable.HashMap.empty[String, SimpleSharedCacheManager]
    override  val globalCache      : SharedCacheManager         = initCaches()
    protected val sharedIdentifiers: SharedCollection[String] = globalCache.get(3, SharedCollection.set[String])
    protected val communicator     : SyncAsyncPacketChannel   = connection.getInjectable(9, ChannelScope.broadcast, SyncAsyncPacketChannel.busy)
    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    init()

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def isConnected(identifier: String): Boolean = getEntity(identifier).isDefined

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    override def newCacheManager(family: String, owner: ConnectionContext): SharedCacheManager = {
        newCacheManager(family, owner.supportIdentifier)
    }

    protected def newCacheManager(family: String, owner: String): SharedCacheManager = {
        if (family == null || owner == null)
            throw new NullPointerException("Family or owner is null.")

        caches.get(family)
                .fold {
                    AppLogger.debug(s"--> CREATING NEW SHARED CACHE MANAGER <$family, $owner>")
                    val cache = new SimpleSharedCacheManager(family, owner, cacheCommunicator)
                    //Will inject all packet that the new cache have possibly missed.
                    caches.put(family, cache)
                    cacheCommunicator.injectStoredPackets()
                    cache: SharedCacheManager
                }(cache => {
                    cache.update()
                    cache
                })
    }

    override def newCacheManager(family: String, owner: ExternalConnection): SharedCacheManager = {
        newCacheManager(family, owner.boundIdentifier)
    }

    protected def createEntity0(identifier: String, communicationChannel: SyncAsyncPacketChannel): NetworkEntity

    protected def createEntity(identifier: String): NetworkEntity = {
        if (identifier == connection.supportIdentifier) {
            return connectionEntity
        }

        val channel = communicator.subInjectable(Array(identifier), SyncAsyncPacketChannel.busy, true)
        val ent     = createEntity0(identifier, channel)
        ent
    }

    private def initCaches(): SharedCacheManager = {
        cacheCommunicator.addAsyncListener((packet, coords) => {
            packet match {
                case WrappedPacket(family, subPacket) =>
                    caches.get(family)
                            //If the caches does not contains the family tag, this mean that it could possibly be
                            //opened in the future, so received packets will be stored and injected every
                            //time a cache opens.
                            .fold(cacheCommunicator.storePacket(WrappedPacket("req", packet), coords))(cache => {
                                cache.handlePacket(subPacket, coords)
                            })
            }
        })
        newCacheManager("Global Cache", serverIdentifier)
    }

    private def init(): Unit = {
        sharedIdentifiers.addListener((_, _, _) => AppLogger.debug(s"SharedIdentifiers Updated : $sharedIdentifiers"))
        connection.translator.updateCache(globalCache)
    }

}

