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
import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.engine.gnom.cache.SharedCacheDistantManager
import fr.linkit.engine.gnom.network.NetworkDataTrunk.CacheManagerInfo
import fr.linkit.engine.gnom.network.statics.StaticAccesses
import fr.linkit.engine.internal.util.ConsumerContainer

import java.sql.Timestamp
import scala.collection.mutable

//FIXME OriginManagers and DistantManagers can be bypassed
class NetworkDataTrunk private(network: AbstractNetwork, val startUpDate: Timestamp) {


    private               val engines           = mutable.HashMap.empty[String, DefaultEngine]
    private               val caches            = mutable.HashMap.empty[String, (SharedCacheManager, Array[Int])]
    private[network] lazy val staticAccesses    = new StaticAccesses(network)
    private               val onNewEngineEvents = ConsumerContainer[Engine]()

    def this(network: AbstractNetwork) {
        this(network, new Timestamp(System.currentTimeMillis()))
    }

    def toBundle: NetworkDataBundle = {
        NetworkDataBundle(engines.keys.toArray, caches.map(p => toCacheData(p._2)).toArray, startUpDate, network)
    }

    private def toCacheData(pair: (SharedCacheManager, Array[Int])): CacheManagerInfo = {
        val (manager: SharedCacheManager, storePath: Array[Int]) = pair
        CacheManagerInfo(manager.family, manager.ownerID, storePath)
    }

    def newEngine(engineIdentifier: String): Engine = engines.synchronized {
        if (engines.contains(engineIdentifier))
            throw new IllegalArgumentException("This engine already exists !")
        val current      = ExecutorEngine.currentEngine
        val cacheManager = {
            if (network.currentEngine ne current) getDistantCache(engineIdentifier)
            else network.newCacheManager(engineIdentifier)
        }
        addEngine(new DefaultEngine(engineIdentifier, cacheManager, network))
    }

    def removeEngine(engine: Engine): Unit = engines -= engine.identifier

    def findCache(family: String): Option[SharedCacheManager] = {
        caches.get(family).map(_._1)
    }

    private[network] def addCacheManager(manager: SharedCacheManager, storePath: Array[Int]): Unit = {
        if (caches.contains(manager.family))
            throw new Exception("Cache Manager already exists")
        caches.put(manager.family, (manager, storePath))
    }

    def findEngine(identifier: String): Option[Engine] = engines.synchronized {
        engines.get(identifier)
    }

    def listEngines: List[Engine] = engines.synchronized(engines.values.toList)

    def countConnection: Int = engines.size

    protected def addEngine(engine: DefaultEngine): DefaultEngine = {
        engines.put(engine.identifier, engine) match {
            case None    => onNewEngineEvents.applyAll(engine)
            case Some(_) =>
        }

        engine
    }

    private def getDistantCache(engineIdentifier: String): SharedCacheManager = caches.getOrElseUpdate(engineIdentifier, {
        val code  = engineIdentifier.hashCode
        val store = network.networkStore.createStore(code)
        val omc   = network.objectManagementChannel
        (new SharedCacheDistantManager(engineIdentifier, engineIdentifier, network, omc, store), store.trafficPath)
    })._1

    //When the trunk gets deserialized, due to the TypePersistence that will deserialize the trunk,
    // the engines in it will not come in their synchronized version,
    // so for each engines already present on the object's creation,
    //we add them in order to apply the NetworkContract.bhv that will synchronize the new engines

    def reinjectEngines(): this.type = engines.synchronized {
        if (engines.nonEmpty) engines.values.foreach(e => {
            addEngine(e).classMappings
        })
        this
    }

    private[network] def onNewEngine(f: Engine => Unit): Unit = {
        onNewEngineEvents += f
    }

}

object NetworkDataTrunk {

    case class CacheManagerInfo(family: String, owner: String, storePath: Array[Int])

    def fromData(data: NetworkDataBundle): NetworkDataTrunk = {
        val network = data.network
        val trunk   = new NetworkDataTrunk(network)
        data.engines.foreach { identifier =>
            trunk.addEngine(new DefaultEngine(identifier, trunk.getDistantCache(identifier), network))
        }
        data.caches.foreach(info => {
            import info._
            trunk.findCache(family).getOrElse {
                val store = getStore(network.connection, storePath)
                val omc   = network.objectManagementChannel
                val cache = new SharedCacheDistantManager(family, owner, network, omc, store)
                trunk.addCacheManager(cache, storePath)
            }
        })
        trunk
    }

    private def getStore(connection: ConnectionContext, path: Array[Int]): PacketInjectableStore = {
        var store: PacketInjectableStore = connection.traffic
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

}
