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

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.RequestPacketBundle
import fr.linkit.api.connection.packet.traffic.PacketInjectableStore
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.SharedCacheOriginManager
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, SynchronizedObjectBehaviorStoreBuilder}
import fr.linkit.engine.connection.cache.obj.invokation.ExecutorEngine
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.SimpleRequestPacketChannel

import java.sql.Timestamp

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    protected val networkStore    : PacketInjectableStore = connection.createStore(0)
    private   val cacheManagerChannel                     = networkStore.getInjectable(1, SimpleRequestPacketChannel, ChannelScopes.discardCurrent)
    private   val currentIdentifier                       = connection.currentIdentifier
    override  val globalCache     : SharedCacheManager    = createGlobalCache
    protected val trunk           : NetworkDataTrunk      = retrieveDataTrunk(getEngineStoreBehaviors)
    override  val connectionEngine: Engine                = trunk.newEngine(currentIdentifier)
    postInit()

    override def serverEngine: Engine = trunk.findEngine(serverIdentifier).getOrElse {
        throw new NoSuchElementException("Server Engine not found.")
    }

    override def startUpDate: Timestamp = trunk.startUpDate

    override def listEngines: List[Engine] = trunk.listEngines

    override def countConnections: Int = trunk.countConnection

    override def findEngine(identifier: String): Option[Engine] = trunk.findEngine(identifier)

    override def isConnected(identifier: String): Boolean = findEngine(identifier).isDefined

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        trunk.findCache(family)
    }

    override def attachToCacheManager(family: String): SharedCacheManager = {
        findCacheManager(family).getOrElse(declareNewCacheManager(family))
    }

    override def declareNewCacheManager(family: String): SharedCacheManager = {
        if (trunk.findCache(family).isDefined)
            throw new CacheManagerAlreadyDeclaredException(s"Cache of family $family is already opened.")
        AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: --> CREATING NEW SHARED CACHE MANAGER <$family>")
        val store   = networkStore.createStore(family.hashCode)
        val manager = new SharedCacheOriginManager(family, this, store)

        trunk.addCacheManager(manager)
        //channel.injectStoredBundles()
        manager
    }

    protected def retrieveDataTrunk(behaviors: ObjectBehaviorStore): NetworkDataTrunk

    protected def createGlobalCache: SharedCacheManager

    protected def handleRequest(bundle: RequestPacketBundle): Unit = {
        bundle.packet.nextPacket[Packet] match {
            case other =>
                throw UnexpectedPacketException(s"Unknown request '$other'.")
        }
    }

    private def postInit(): Unit = {
        ExecutorEngine.setCurrentEngine(connectionEngine)
        connection.translator.initNetwork(this)
        cacheManagerChannel.addRequestListener(handleRequest)
    }

    private def getEngineStoreBehaviors: ObjectBehaviorStore = {
        new SynchronizedObjectBehaviorStoreBuilder(AnnotationBasedMemberBehaviorFactory) {

        }.build
    }
}
