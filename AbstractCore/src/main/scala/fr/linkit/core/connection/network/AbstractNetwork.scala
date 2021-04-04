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

import fr.linkit.api.connection.network.cache.{CacheOpenBehavior, SharedCacheManager}
import fr.linkit.api.connection.network.{Network, NetworkEntity}
import fr.linkit.api.connection.packet.traffic.ChannelScope
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.linkit.core.connection.network.cache.{NetworkSharedCacheManager, SyncAsyncSender}
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.traffic.channel.request.RequestPacketChannel
import fr.linkit.core.connection.packet.traffic.channel.{AbstractPacketChannel, SyncAsyncPacketChannel}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    private   val cacheCommunicator                           = connection.getInjectable(11, ChannelScope.broadcast, new SyncAsyncSender(_))
    private   val cacheRequestChannel                         = connection.getInjectable(12, ChannelScope.broadcast, RequestPacketChannel)
    private   val caches                                      = mutable.HashMap.empty[String, NetworkSharedCacheManager]
    override  val globalCache      : SharedCacheManager       = initCaches()
    protected val sharedIdentifiers: SharedCollection[String] = globalCache.getCache(3, SharedCollection.set[String], CacheOpenBehavior.AWAIT_OPEN)
    protected val communicator     : SyncAsyncPacketChannel   = connection.getInjectable(9, ChannelScope.broadcast, SyncAsyncPacketChannel.busy)
    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    postInit()

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
                    AppLogger.debug(s"$currentTasksId <> ${connection.supportIdentifier}: --> CREATING NEW SHARED CACHE MANAGER <$family, $owner>")
                    val cache = new NetworkSharedCacheManager(family, owner, cacheCommunicator, cacheRequestChannel)
                    //Will inject all packet that the new cache have possibly missed.
                    caches.synchronized {
                        AppLogger.debug(s"$currentTasksId <> ${connection.supportIdentifier}: PUTTING CACHE <$family, $owner> INTO CACHES")
                        caches.put(family, cache)
                        AppLogger.debug(s"$currentTasksId <> ${connection.supportIdentifier}: CACHES <$family, $owner> IS NOW : $caches")
                    }
                    cacheCommunicator.injectStoredPackets()
                    cache: SharedCacheManager
                }(cache => {
                    AppLogger.debug(s"$currentTasksId <> ${connection.supportIdentifier}: <$family, $owner> UpDaTiNg CaChE")
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

        val channel = communicator.subInjectable(Array(identifier), SyncAsyncPacketChannel.busy, transparent = true)
        val ent     = createEntity0(identifier, channel)
        ent
    }

    private def initCaches(): SharedCacheManager = {
        def findCacheToNotify(channel: AbstractPacketChannel, packet: Packet, coords: DedicatedPacketCoordinates)
                  (notifyAction: (Packet, NetworkSharedCacheManager) => Unit): Unit = {
            packet match {
                case WrappedPacket(family, subPacket) =>
                    val opt = caches.synchronized {
                        AppLogger.warn(s"$currentTasksId <> ${connection.supportIdentifier}: FINDING CACHE '$family' FOR PACKET $packet into $caches")
                        caches.get(family)
                    }
                    opt
                            //If the caches does not contains the family tag, this mean that it could possibly be
                            //opened in the future, so received packets will be stored and injected every
                            //time a cache opens.
                            .fold(channel.storePacket(WrappedPacket("a", packet), coords))(cache => {
                                notifyAction(subPacket, cache)
                            })
            }
        }

        cacheCommunicator.addAsyncListener((packet, coords) => {
            findCacheToNotify(cacheCommunicator, packet, coords) {
                (subPacket, cache) => cache.handleCachePacket(subPacket, coords)
            }
        })

        cacheRequestChannel.addRequestListener((packet, coords, submitter) => {
            findCacheToNotify(cacheRequestChannel, packet, coords) {
                (_, cache) => cache.handleRequest(packet, coords, submitter)
            }
        })

        newCacheManager(s"Global Cache", serverIdentifier)
    }

    private def postInit(): Unit = {
        sharedIdentifiers.addListener((_, _, _) => AppLogger.debug(s"$currentTasksId <> ${connection.supportIdentifier}: SharedIdentifiers Updated : $sharedIdentifiers"))
        connection.translator.updateCache(globalCache)
    }

}

