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
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.{SharedCacheManager, SharedCacheManagerReference}
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.referencing.linker.RemainingNetworkObjectLinker
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.SharedCacheManagersLinker
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.gnom.referencing.linker.MapNetworkObjectLinker
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp

abstract class AbstractNetwork(override val connection: ConnectionContext) extends Network {

    override       val reference      = NetworkReference
    protected      val currentTag     = NameTag(connection.currentName)
    private lazy   val engine0        = initCurrentEngine()
    private lazy   val staticAccesses = trunk.staticAccesses
    lazy           val traffic        = connection.traffic.asInstanceOf[AbstractPacketTraffic] //FIXME Ugly
    protected lazy val networkStore   = traffic.createStore(0)
    private lazy   val tnol           = traffic.getTrafficObjectLinker
    private lazy   val rnol           = new MapNetworkObjectLinker(traffic.getObjectManagementChannel, this) with RemainingNetworkObjectLinker
    private lazy   val scnol          = new SharedCacheManagersLinker(this, traffic.getObjectManagementChannel)
    override lazy  val gnol           = new GeneralNetworkObjectLinkerImpl(traffic.getObjectManagementChannel, this, scnol, tnol, rnol)
    override lazy  val globalCaches   = createNewCache(GlobalCacheID, Array(GlobalCacheID.hashCode))
    protected lazy val trunk          = initDataTrunk()

    ContractProvider.registerProperties(ObjectsProperty.defaults(this))

    private var trunkInitializing        = false
    private var currentEngineInitializing = false


    def initialize(): Unit = {
        currentEngine //retrieving current engine will start all systems (trunk/gnol/cache management) by cascading
        //serverEngine
    }

    override def retrieveNT(uniqueTag: UniqueTag with NetworkFriendlyEngineTag): NameTag = uniqueTag match {
        case Current     => currentTag
        case nt: NameTag => nt
        case _           => apply(uniqueTag).nameTag
    }

    private def listNameTags(tag: NetworkFriendlyEngineTag): List[NameTag] = tag match {
        case unique: UniqueTag => List(retrieveNT(unique))
        case Nobody            => Nil
        case Everyone          => listAllNts
        case _                 => listEngines(tag).map(_.nameTag)
    }

    override def listNameTags(uniqueTag: NFETSelect): List[NameTag] = uniqueTag match {
        case Select(tag: UniqueTag) => List(retrieveNT(tag))
        case Select(tag)            => listNameTags(tag)
        case Not(tag)               => listAllNts diff listNameTags(tag)
        case Union(a, b)            => listNameTags(a) ::: listNameTags(b)
        case Intersection(a, b)     => listNameTags(a) intersect listNameTags(b)
    }

    private def listAllNts: List[NameTag] = {
        if (isTrunkInitializing || currentEngineInitializing) listInitialNTs
        else listEngines.map(_.nameTag)
    }

    protected def listInitialNTs: List[NameTag]

    override def currentEngine: Engine = engine0

    override def serverEngine: Engine = trunk.findEngine(Server).getOrElse {

        throw new NoSuchElementException("Server Engine not found.")

    }

    override def startUpDate: Timestamp = trunk.startUpDate

    override def listEngines: List[Engine] = {
        if (trunkInitializing) Nil
        else trunk.listEngines
    }

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
        //TODO can be optimized
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
            case Select(tag: UniqueTag) => getEngine(tag).toList
            case Select(tag)            => findEngines(tag, reverseSelection)
            case Not(tag)               => findEngines(tag, !reverseSelection)
            case Union(a, b)            => listEngines(a) ::: listEngines(b)
            case Intersection(a, b)     => listEngines(a) intersect listEngines(b)
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
        trunk.findCacheManager(family)
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

    private def initCurrentEngine(): Engine = {
        val trunk = this.trunk

        currentEngineInitializing = true
        val engine = trunk.newEngine(currentTag.name)
        ExecutorEngine.initDefaultEngine(engine)
        currentEngineInitializing = false
        engine
    }

    private def initDataTrunk(): NetworkDataTrunk = {
        AppLoggers.GNOM.debug("Initialising Network Trunk.")
        trunkInitializing = true
        val trunk = retrieveDataTrunk()
        trunk.reinjectEngines()
        trunkInitializing = false
        AppLoggers.GNOM.debug("Network Trunk Initialised.")
        trunk
    }

    protected def retrieveDataTrunk(): NetworkDataTrunk


    protected def isTrunkInitializing: Boolean = trunkInitializing

    protected def registerSCMReference(ref: SharedCacheManagerReference): Unit = scnol.registerReference(ref)

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