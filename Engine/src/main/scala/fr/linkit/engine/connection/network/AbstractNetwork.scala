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
import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldModifier
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterModifier
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.connection.network.{Engine, Network, NetworkInitialisable}
import fr.linkit.api.connection.packet.persistence.context.MutableReferencedObjectStore
import fr.linkit.api.connection.packet.traffic.PacketInjectableStore
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, ObjectBehaviorBuilder, ObjectBehaviorStoreBuilder}
import fr.linkit.engine.connection.cache.obj.invokation.ExecutorEngine
import fr.linkit.engine.connection.cache.{SharedCacheDistantManager, SharedCacheOriginManager}
import fr.linkit.engine.connection.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.SimpleRequestPacketChannel

import java.sql.Timestamp

abstract class AbstractNetwork(override val connection: ConnectionContext,
                               override val rootRefStore: MutableReferencedObjectStore,
                               privilegedInitialisables: Array[NetworkInitialisable]) extends Network { network =>

    rootRefStore += (10, this)
    privilegedInitialisables.foreach(_.initNetwork(this))
    protected val networkStore    : PacketInjectableStore = connection.createStore(0)
    private   val currentIdentifier                       = connection.currentIdentifier
    override  val globalCache     : SharedCacheManager    = createGlobalCache
    protected val trunk           : NetworkDataTrunk      = retrieveDataTrunk(getEngineStoreBehaviors)
    override  val connectionEngine: Engine                = trunk.newEngine(currentIdentifier)
    runContract()

    override def serverEngine: Engine = trunk.findEngine(serverIdentifier).getOrElse {
        throw new NoSuchElementException("Server Engine not found.")
    }

    override def startUpDate: Timestamp = trunk.startUpDate

    override def listEngines: List[Engine] = trunk.listEngines

    override def countConnections: Int = trunk.countConnection

    override def findEngine(identifier: String): Option[Engine] = trunk.findEngine(identifier)

    override def isConnected(identifier: String): Boolean = findEngine(identifier).isDefined

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        if (family == GlobalCacheID)
            return Some(globalCache)
        trunk.findCache(family)
    }

    override def attachToCacheManager(family: String): SharedCacheManager = {
        findCacheManager(family).getOrElse(declareNewCacheManager(family))
    }

    override def declareNewCacheManager(family: String): SharedCacheManager = {
        if (trunk.findCache(family).isDefined)
            throw new CacheManagerAlreadyDeclaredException(s"Cache of family $family is already opened.")

        val manager = newCacheManager(family)
        trunk.addCacheManager(manager)
        manager
    }

    private[network] def newCacheManager(family: String): SharedCacheManager = {
        AppLogger.vDebug(s"$currentTasksId <> ${connection.currentIdentifier}: --> CREATING NEW SHARED CACHE MANAGER <$family>")
        val store = networkStore.createStore(family.hashCode)
        new SharedCacheOriginManager(family, this, store)
    }

    protected def retrieveDataTrunk(behaviors: ObjectBehaviorStore): NetworkDataTrunk

    protected def createGlobalCache: SharedCacheManager

    /*protected def handleRequest(bundle: RequestPacketBundle): Unit = {
        bundle.packet.nextPacket[Packet] match {
            case other => throw UnexpectedPacketException(s"Unknown request '$other'.")
        }
    }*/

    private def runContract(): Unit = {
        ExecutorEngine.setCurrentEngine(connectionEngine)
        //cacheManagerChannel.addRequestListener(handleRequest)
    }

    private def transformToDistant(cache: SharedCacheOriginManager): SharedCacheDistantManager = {
        val family = cache.family
        val store  = networkStore.createStore(family.hashCode)
        new SharedCacheDistantManager(family, cache.ownerID, this, store)
    }

    private def getEngineStoreBehaviors: ObjectBehaviorStore = {
        new ObjectBehaviorStoreBuilder(AnnotationBasedMemberBehaviorFactory) {
            behaviors += new ObjectBehaviorBuilder[SharedCacheManager]() {
                asParameter = new ParameterModifier[SharedCacheManager] {
                    override def forLocalComingFromRemote(receivedParam: SharedCacheManager, invocation: LocalMethodInvocation[_], remote: Engine): SharedCacheManager = {
                        receivedParam match {
                            case origin: SharedCacheOriginManager => transformToDistant(origin)
                            case _                                => receivedParam
                        }
                    }
                }
            }
            behaviors += new ObjectBehaviorBuilder[DefaultEngine]() {
                asField = new FieldModifier[DefaultEngine] {
                    override def forLocalComingFromRemote(receivedField: DefaultEngine, containingObject: SynchronizedObject[_], remote: Engine): DefaultEngine = {
                        val identifier = receivedField.identifier
                        new DefaultEngine(identifier, findCacheManager(identifier).get)
                    }
                }
            }
        }.build
    }
}

object AbstractNetwork {

    final val GlobalCacheID = "Global Cache"
}