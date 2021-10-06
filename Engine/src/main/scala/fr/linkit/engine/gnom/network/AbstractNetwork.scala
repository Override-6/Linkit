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

package fr.linkit.engine.gnom.network

import java.sql.Timestamp

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterModifier
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.gnom.network.{Engine, Network, NetworkInitialisable, NetworkReference}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.behavior.{AnnotationBasedMemberBehaviorFactory, ObjectBehaviorBuilder, ObjectBehaviorStoreBuilder}
import fr.linkit.engine.gnom.cache.sync.invokation.ExecutorEngine
import fr.linkit.engine.gnom.cache.{SharedCacheDistantManager, SharedCacheManagerLinker, SharedCacheOriginManager}
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID

abstract class AbstractNetwork(override val connection: ConnectionContext,
                               omc: ObjectManagementChannel,
                               privilegedInitialisables: Array[NetworkInitialisable]) extends Network {

    //rootRefStore += (10, this)
    privilegedInitialisables.foreach(_.initNetwork(this))
    override  val objectManagementChannel: ObjectManagementChannel    = omc
    private   val scnol                  : SharedCacheManagerLinker   = new SharedCacheManagerLinker(this, omc)
    private   val gnol                   : GeneralNetworkObjectLinker = new GeneralNetworkObjectLinker(omc, this, scnol, null)
    protected val networkStore           : PacketInjectableStore      = connection.createStore(0)
    private   val currentIdentifier      : String                     = connection.currentIdentifier
    override  val globalCache            : SharedCacheManager         = createGlobalCache
    protected val trunk                  : NetworkDataTrunk           = retrieveDataTrunk(getEngineStoreBehaviors)
    override  val connectionEngine       : Engine                     = trunk.newEngine(currentIdentifier)
    override  val reference              : NetworkReference           = new NetworkReference()
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
        if (family == GlobalCacheID)
            return Some(globalCache)
        trunk.findCache(family)
    }

    override def getCacheManager(family: String): SharedCacheManager = {
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
        val store   = networkStore.createStore(family.hashCode)
        val manager = new SharedCacheOriginManager(family, this, store)
        scnol.registerReference(manager.reference)
        manager
    }

    protected def retrieveDataTrunk(behaviors: ObjectBehaviorStore): NetworkDataTrunk

    protected def createGlobalCache: SharedCacheManager

    private def postInit(): Unit = {
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