/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.network

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network, NetworkReference}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.referencing.linker.{GeneralNetworkObjectLinker, RemainingNetworkObjectLinker}
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.cache.{SharedCacheDistantManager, SharedCacheManagersLinker, SharedCacheOriginManager}
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.statics.StaticAccesses
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.gnom.referencing.linker.MapNetworkObjectLinker
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp

abstract class AbstractNetwork(traffic: AbstractPacketTraffic) extends Network {

    override           val reference                                           = NetworkReference
    override           val connection             : ConnectionContext          = traffic.connection
    protected[network] val objectManagementChannel: ObjectManagementChannel    = traffic.getObjectManagementChannel
    protected[network] val networkStore           : PacketInjectableStore      = traffic.createStore(0)
    private            val currentIdentifier      : String                     = connection.currentIdentifier
    private            val tnol                                                = traffic.getTrafficObjectLinker
    private            val rnol                                                = new MapNetworkObjectLinker(objectManagementChannel) with RemainingNetworkObjectLinker
    private            val scnol                  : SharedCacheManagersLinker  = new SharedCacheManagersLinker(this, objectManagementChannel)
    override lazy      val gnol                   : GeneralNetworkObjectLinker = new GeneralNetworkObjectLinkerImpl(objectManagementChannel, this, scnol, tnol, rnol)
    override lazy      val globalCaches           : SharedCacheManager         = createGlobalCache
    protected lazy     val trunk                  : NetworkDataTrunk           = initDataTrunk()
    private var engine0                           : Engine                     = _
    private var staticAccesses                    : StaticAccesses             = _

    private var trunkInitializing = false

    override def currentEngine: Engine = engine0

    override def serverEngine: Engine = trunk.findEngine(serverIdentifier).getOrElse {
        throw new NoSuchElementException("Server Engine not found.")
    }

    override def startUpDate: Timestamp = trunk.startUpDate

    override def listEngines: List[Engine] = trunk.listEngines

    override def countConnections: Int = trunk.countConnection

    override def findEngine(identifier: String): Option[Engine] = {
        if (trunkInitializing)
            return None
        trunk.findEngine(identifier)
    }

    override def isConnected(identifier: String): Boolean = findEngine(identifier).isDefined

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        if (family == GlobalCacheID)
            return Some(globalCaches)
        if (trunkInitializing)
            return None
        trunk.findCache(family)
    }

    override def attachToCacheManager(family: String): SharedCacheManager = {
        findCacheManager(family).getOrElse(declareNewCacheManager(family))
    }

    override def declareNewCacheManager(family: String): SharedCacheManager = {
        if (trunkInitializing)
            throw new UnsupportedOperationException("Trunk is initializing.")
        if (trunk.findCache(family).isDefined)
            throw new CacheManagerAlreadyDeclaredException(s"Cache of family $family is already opened.")
        newCacheManager(family)
    }

    override def getStaticAccess(id: Int): StaticAccess = {
        staticAccesses.getStaticAccess(id)
    }

    override def newStaticAccess(id: Int, contract: ContractDescriptorData): StaticAccess = {
        staticAccesses.newStaticAccess(id, contract)
    }

    override def newStaticAccess(id: Int): StaticAccess = {
        newStaticAccess(id, EmptyContractDescriptorData)
    }

    protected def addCacheManager(manager: SharedCacheManager, storePath: Array[Int]): Unit = {
        trunk.addCacheManager(manager, storePath)
    }

    private[network] def newCacheManager(family: String): SharedCacheManager = {
        val store       = networkStore.createStore(family.hashCode)
        val manager     = new SharedCacheOriginManager(family, this, objectManagementChannel, store)
        val trafficPath = store.trafficPath
        scnol.registerReference(manager.reference)
        addCacheManager(manager, trafficPath)
        manager
    }

    private def initDataTrunk(): NetworkDataTrunk = {
        AppLoggers.GNOM.debug("Initialising Network Trunk.")
        trunkInitializing = true
        val trunk = retrieveDataTrunk()
        trunkInitializing = false
        AppLoggers.GNOM.debug("Network Trunk Initialised.")
        trunk
    }

    protected def retrieveDataTrunk(): NetworkDataTrunk

    protected def createGlobalCache: SharedCacheManager

    def initialize(): this.type = {
        //init those lazy vals, do not change the order!
        AppLoggers.GNOM.info("Initialising Network.")
        ContractProvider.registerProperties(ObjectsProperty.defaults(this))
        AppLoggers.GNOM.debug("Initialising GNOL and Global Cache.")
        gnol
        globalCaches

        AppLoggers.GNOM.debug("Finalizing Network Initialisation.")
        engine0 = trunk.newEngine(currentIdentifier)
        trunk.reinjectEngines()
        staticAccesses = trunk.staticAccesses

        ExecutorEngine.initDefaultEngine(currentEngine)
        AppLoggers.GNOM.debug("Network Initialised.")
        this
    }

    //TODO Private this or find an alternative ! - not private because used in NetworkContract.bhv contract file
    def transformToDistant(cache: SharedCacheOriginManager): SharedCacheDistantManager = {
        val family = cache.family
        val store  = networkStore.createStore(family.hashCode)
        new SharedCacheDistantManager(family, cache.ownerID, this, objectManagementChannel, store)
    }

    private var isMappingCacheInitialising                                          = false
    private var mappingsCacheOpt: Option[ConnectedObjectCache[RemoteClassMappings]] = None

    private[network] def mappingsCache = {
        if (mappingsCacheOpt.isEmpty && !isMappingCacheInitialising) {
            isMappingCacheInitialising = true
            val contract = ContractProvider("NetworkContract", ObjectsProperty.defaults(this))
            mappingsCacheOpt = Some(globalCaches.attachToCache(2, DefaultConnectedObjectCache[RemoteClassMappings](contract)))
            isMappingCacheInitialising = false
        }
        mappingsCacheOpt
    }

    override def onNewEngine(f: Engine => Unit): Unit = trunk.onNewEngine(f)
}

object AbstractNetwork {

    final val GlobalCacheID = "Global Cache"
}