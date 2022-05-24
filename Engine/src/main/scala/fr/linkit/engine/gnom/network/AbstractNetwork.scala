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
import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network, NetworkReference}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.reference.linker.{GeneralNetworkObjectLinker, RemainingNetworkObjectsLinker}
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.cache.{SharedCacheDistantManager, SharedCacheManagerLinker, SharedCacheOriginManager}
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.statics.StaticAccesses
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.gnom.reference.linker.MapNetworkObjectsLinker
import fr.linkit.engine.internal.language.bhv.Contract
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp

abstract class AbstractNetwork(traffic: AbstractPacketTraffic) extends Network {
    
    override           val reference              : NetworkReference           = new NetworkReference()
    override           val connection             : ConnectionContext          = traffic.connection
    override           val objectManagementChannel: ObjectManagementChannel    = traffic.getObjectManagementChannel
    protected[network] val networkStore           : PacketInjectableStore      = traffic.createStore(0)
    private            val currentIdentifier      : String                     = connection.currentIdentifier
    private            val tnol                                                = traffic.getTrafficObjectLinker
    private            val rnol                                                = new MapNetworkObjectsLinker(objectManagementChannel) with RemainingNetworkObjectsLinker
    private            val scnol                  : SharedCacheManagerLinker   = new SharedCacheManagerLinker(this, objectManagementChannel)
    override lazy      val gnol                   : GeneralNetworkObjectLinker = new GeneralNetworkObjectLinkerImpl(objectManagementChannel, this, scnol, tnol, Some(rnol))
    override lazy      val globalCache            : SharedCacheManager         = createGlobalCache
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
            return Some(globalCache)
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
        val manager     = new SharedCacheOriginManager(family, this, store)
        val trafficPath = store.trafficPath
        scnol.registerReference(manager.reference)
        addCacheManager(manager, trafficPath)
        manager
    }
    
    private def initDataTrunk(): NetworkDataTrunk = {
        trunkInitializing = true
        val trunk = retrieveDataTrunk()
        trunkInitializing = false
        trunk
    }
    
    protected def retrieveDataTrunk(): NetworkDataTrunk
    
    protected def createGlobalCache: SharedCacheManager
    
    def initialize(): this.type = {
        //init those lazy vals, do not change the order!
        gnol
        globalCache
        
        trunk.reinjectEngines()
        engine0 = trunk.newEngine(currentIdentifier)
        staticAccesses = trunk.staticAccesses
        engine0.asInstanceOf[DefaultEngine].classMappings
        
        ExecutorEngine.initDefaultEngine(currentEngine)
        this
    }
    
    //TODO Private this or find an alternative ! - not private because used in NetworkContract.bhv contract file
    def transformToDistant(cache: SharedCacheOriginManager): SharedCacheDistantManager = {
        val family = cache.family
        val store  = networkStore.createStore(family.hashCode)
        new SharedCacheDistantManager(family, cache.ownerID, this, store)
    }
    
    private var isMappingCacheInitialising                                             = false
    private var mappingsCacheOpt: Option[SynchronizedObjectCache[RemoteClassMappings]] = None
    
    private[network] def mappingsCache = {
        if (mappingsCacheOpt.isEmpty && !isMappingCacheInitialising) {
            isMappingCacheInitialising = true
            val contract = Contract("NetworkContract")(this)
            mappingsCacheOpt = Some(globalCache.attachToCache(2, DefaultSynchronizedObjectCache[RemoteClassMappings](contract)))
            isMappingCacheInitialising = false
        }
        mappingsCacheOpt
    }
    
    override def onNewEngine(f: Engine => Unit): Unit = trunk.onNewEngine(f)
}

object AbstractNetwork {
    
    final val GlobalCacheID = "Global Cache"
}