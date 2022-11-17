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
import fr.linkit.api.gnom.cache.{SharedCacheManager, SharedCacheManagerReference}
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.referencing.linker.{GeneralNetworkObjectLinker, RemainingNetworkObjectLinker}
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.SharedCacheManagersLinker
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.statics.StaticAccesses
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.gnom.referencing.linker.MapNetworkObjectLinker
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp

abstract class AbstractNetwork(traffic: AbstractPacketTraffic) extends Network {

    override       val reference                                           = NetworkReference
    override       val connection             : ConnectionContext          = traffic.connection
    protected      val objectManagementChannel: ObjectManagementChannel    = traffic.getObjectManagementChannel
    protected      val networkStore           : PacketInjectableStore      = traffic.createStore(0)
    private        val currentIdentifier      : String                     = connection.currentName
    private        val tnol                                                = traffic.getTrafficObjectLinker
    private        val rnol                                                = new MapNetworkObjectLinker(objectManagementChannel) with RemainingNetworkObjectLinker
    private        val scnol                  : SharedCacheManagersLinker  = new SharedCacheManagersLinker(this, objectManagementChannel)
    override lazy  val gnol                   : GeneralNetworkObjectLinker = new GeneralNetworkObjectLinkerImpl(objectManagementChannel, this, scnol, tnol, rnol)
    override lazy  val globalCaches           : SharedCacheManager         = createNewCache(GlobalCacheID, Array(GlobalCacheID.hashCode))
    protected lazy val trunk                  : NetworkDataTrunk           = initDataTrunk()
    private var engine0                       : Engine                     = _
    private var staticAccesses                : StaticAccesses             = _

    private var trunkInitializing = false


    override def currentEngine: Engine = engine0

    override def serverEngine: Engine = trunk.findEngine(Server).getOrElse {
        throw new NoSuchElementException("Server Engine not found.")
    }

    override def startUpDate: Timestamp = trunk.startUpDate

    override def listEngines: List[Engine] = trunk.listEngines

    override def countConnections: Int = trunk.countConnection

    override def getEngine(tag: UniqueTag with NetworkFriendlyEngineTag): Option[Engine] = {
        if (trunkInitializing)
            return None
        tag match {
            case name: NameTag     => trunk.findEngine(name)
            case id: IdentifierTag => trunk.findEngine(id)
            case Current           => Some(currentEngine)
            case _                 => throw new IllegalTagException(s"Unknown tag '$tag'.")
        }
    }

    override def exists(selection: NFETSelect): Boolean = selection match {
        case Select(Nobody) => true
        case selection      => listEngines.exists(_.isIncluded(selection))
    }


    override def isIncluded(a: NFETSelect, b: NFETSelect): Boolean = {
        //TODO optimize
        listEngines(a).forall(_.isIncluded(b))
    }

    override def listEngines(tag: NFETSelect): List[Engine] = {
        if (trunkInitializing)
            return Nil
        findEngines(tag, false)
    }

    private def findEngines(selection: NFETSelect, reverseSelection: Boolean): List[Engine] = {
        val allEngines      = listEngines
        val selectedEngines = selection match {
            case Select(tag)        => findEngines(tag, reverseSelection)
            case Not(tag)           => findEngines(tag, !reverseSelection)
            case Union(a, b)        => listEngines(a) ::: listEngines(b)
            case Intersection(a, b) => listEngines(a) intersect listEngines(b)
        }
        if (reverseSelection)
            allEngines.diff(selectedEngines)
        else selectedEngines
    }

    private[network] def createNewCache(family: String, managerChannelPath: Array[Int]): SharedCacheManager = {
        createNewCache0(family, managerChannelPath)
    }

    protected def createNewCache0(family: String, managerChannelPath: Array[Int]): SharedCacheManager

    override def findCacheManager(family: String): Option[SharedCacheManager] = {
        if (family == GlobalCacheID)
            return Some(globalCaches)
        if (trunkInitializing)
            return None
        trunk.findCache(family)
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

    private def initDataTrunk(): NetworkDataTrunk = {
        AppLoggers.GNOM.debug("Initialising Network Trunk.")
        trunkInitializing = true
        val trunk = retrieveDataTrunk()
        trunkInitializing = false
        AppLoggers.GNOM.debug("Network Trunk Initialised.")
        trunk
    }

    protected def retrieveDataTrunk(): NetworkDataTrunk


    protected def isTrunkInitializing: Boolean = trunkInitializing

    protected def registerSCMReference(ref: SharedCacheManagerReference): Unit = {
        scnol.registerReference(ref)
    }

    protected def getStore(path: Array[Int]): PacketInjectableStore = {
        var store: PacketInjectableStore = traffic
        var i                            = 0
        while (i < path.length) {
            val id = path(i)
            store = store.findStore(path(i)).getOrElse {
                store.createStore(id)
            }
            i += 1
        }
        store
    }

    def initialize(): this.type = {
        //init those lazy vals, do not change the order!
        AppLoggers.Connection.info("Initialising Network.")
        ContractProvider.registerProperties(ObjectsProperty.defaults(this))
        AppLoggers.Connection.debug("Initialising GNOL and Global Cache.")
        gnol

        AppLoggers.Connection.debug("Finalizing Network Initialisation.")
        engine0 = trunk.newEngine(currentIdentifier)
        trunk.reinjectEngines()
        staticAccesses = trunk.staticAccesses

        ExecutorEngine.initDefaultEngine(currentEngine)
        AppLoggers.Connection.debug("Network Initialised.")
        this
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