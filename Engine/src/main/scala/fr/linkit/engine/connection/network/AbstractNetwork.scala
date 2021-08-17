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

import java.sql.Timestamp

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule._
import fr.linkit.api.connection.cache.obj.behavior.member.MethodParameterBehavior
import fr.linkit.api.connection.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.RequestPacketBundle
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.behavior.SynchronizedObjectBuilder.MethodControl
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, SynchronizedObjectBuilder, SynchronizedObjectStoreBuilder}
import fr.linkit.engine.connection.cache.{SharedCacheDistantManager, SharedCacheOriginManager}
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket.StringPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.SimpleRequestPacketChannel

import scala.collection.mutable

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    private   val caches                               = mutable.HashMap.empty[String, SharedCacheManager]
    private   val cacheManagerChannel                  = connection.getInjectable(10, ChannelScopes.discardCurrent, SimpleRequestPacketChannel)
    private   val currentIdentifier                    = connection.currentIdentifier
    override  val globalCache     : SharedCacheManager = createGlobalCache
    protected val engineStore     : EngineStore        = retrieveEngineStore(getEngineStoreBehaviors)
    override  val connectionEngine: Engine             = engineStore.newEngine(currentIdentifier, declareNewCacheManager(currentIdentifier))
    postInit()

    override def serverEngine: Engine = engineStore.findEngine(serverIdentifier).getOrElse {
        throw new NoSuchElementException("Server Engine not found.")
    }

    override def startUpDate: Timestamp = engineStore.startUpDate

    override def listEngines: List[Engine] = engineStore.listEngines

    override def findEngine(identifier: String): Option[Engine] = engineStore.findEngine(identifier)

    override def isConnected(identifier: String): Boolean = findEngine(identifier).isDefined

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        caches.get(family).orElse(findDistantCacheManager(family))
    }

    override def attachToCacheManager(family: String): SharedCacheManager = {
        findCacheManager(family).getOrElse(declareNewCacheManager(family))
    }

    override def declareNewCacheManager(family: String): SharedCacheManager = {
        if (caches.contains(family))
            throw new CacheManagerAlreadyDeclaredException(s"Cache of family $family is already opened.")
        AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: --> CREATING NEW SHARED CACHE MANAGER <$family>")
        val channel = connection.getInjectable(family.hashCode, ChannelScopes.discardCurrent, SimpleRequestPacketChannel)
        val cache   = new SharedCacheOriginManager(family, this, channel)

        //Will inject all packet that the new cache have possibly missed.
        caches.synchronized {
            caches.put(family, cache)
        }
        channel.injectStoredBundles()
        cache
    }

    protected def retrieveEngineStore(behaviors: SynchronizedObjectBehaviorStore): EngineStore

    protected def createGlobalCache: SharedCacheManager

    protected def findDistantCacheManager(family: String, owner: String): Option[SharedCacheManager] = {
        //TODO Pretty slow, and does not ensure that only one engine is the owner of the family.
        // A registry where all opened managers are stored may be done.
        if (owner == connection.currentIdentifier)
            throw new IllegalArgumentException("Can't find a distant manager that would be owned by this engine. (use find or declareCacheManager instead)")
        if (caches.contains(family))
            return Some(caches(family))
        val isSet = cacheManagerChannel.makeRequest(ChannelScopes.include(owner))
            .addPacket(StringPacket(family))
            .submit()
            .nextResponse
            .nextPacket[BooleanPacket].value

        if (isSet) {
            val channel = connection.getInjectable(family.hashCode, ChannelScopes.discardCurrent, SimpleRequestPacketChannel)
            val manager = new SharedCacheDistantManager(family, owner, this, channel)
            caches.put(family, manager)
            Some(manager)
        } else None
    }

    protected def findDistantCacheManager(family: String): Option[SharedCacheManager] = {
        listEngines.filter(_.identifier != connection.currentIdentifier)
            .flatMap(c => findDistantCacheManager(family, c.identifier))
            .headOption
    }

    protected def handleRequest(bundle: RequestPacketBundle): Unit = {
        val response = bundle.responseSubmitter
        bundle.packet.nextPacket[Packet] match {
            //Request for testing the presence of a CacheManager on this machine.
            case StringPacket(managerFamily: String) =>
                response.addPacket(BooleanPacket(caches.contains(managerFamily))).submit()
            case other                               => throw UnexpectedPacketException(s"Unknown request '$other'.")
        }
    }

    private def postInit(): Unit = {
        connection.translator.initNetwork(this)
        cacheManagerChannel.addRequestListener(handleRequest)
    }

    private def transformToDistantCache(manager: SharedCacheManager): SharedCacheManager = {
        val family  = manager.family
        val channel = connection.getInjectable(family.hashCode, ChannelScopes.discardCurrent, SimpleRequestPacketChannel)
        new SharedCacheDistantManager(family, manager.ownerID, this, channel)
    }

    private def getEngineStoreBehaviors: SynchronizedObjectBehaviorStore = {
        new SynchronizedObjectStoreBuilder(AnnotationBasedMemberBehaviorFactory) {
            behaviors += new SynchronizedObjectBuilder[EngineStore]() {
                annotateAllMethods("newEngine") by MethodControl(BROADCAST, synchronizedParams = Seq(MethodParameterBehavior(false, null, null), MethodParameterBehavior[SharedCacheManager](false, null, (manager, comesFromRMI) => if (comesFromRMI) transformToDistantCache(manager) else manager)))
            }
        }.build
    }
}
