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

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterModifier
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network, NetworkReference}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.reference.GeneralNetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.behavior.{AnnotationBasedMemberBehaviorFactory, ObjectBehaviorBuilder, ObjectBehaviorStoreBuilder}
import fr.linkit.engine.gnom.cache.{SharedCacheDistantManager, SharedCacheManagerLinker, SharedCacheOriginManager}
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic

import java.sql.Timestamp

abstract class AbstractNetwork(traffic: AbstractPacketTraffic) extends Network {

    //rootRefStore += (10, this)
    //traffic.context.initNetwork(this)
    override       val reference              : NetworkReference           = new NetworkReference()
    override       val connection             : ConnectionContext          = traffic.connection
    override       val objectManagementChannel: ObjectManagementChannel    = traffic.getObjectManagementChannel
    protected      val networkStore           : PacketInjectableStore      = connection.createStore(0)
    private        val currentIdentifier      : String                     = connection.currentIdentifier
    private        val tnol                                                = traffic.getTrafficObjectLinker
    private lazy   val scnol                  : SharedCacheManagerLinker   = new SharedCacheManagerLinker(this, objectManagementChannel)
    override lazy  val gnol                   : GeneralNetworkObjectLinker = new GeneralNetworkObjectLinkerImpl(objectManagementChannel, this, scnol, tnol)
    override lazy  val globalCache            : SharedCacheManager         = createGlobalCache
    protected lazy val trunk                  : NetworkDataTrunk           = retrieveDataTrunk(getEngineStoreBehaviors)
    private var engine0                       : Engine                     = _

    override def connectionEngine: Engine = engine0

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

    def initialize(): this.type = {
        //init those lazy vals
        gnol
        globalCache
        trunk
        engine0 = trunk.newEngine(currentIdentifier)
        ExecutorEngine.initDefaultEngine(connectionEngine)
        this
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