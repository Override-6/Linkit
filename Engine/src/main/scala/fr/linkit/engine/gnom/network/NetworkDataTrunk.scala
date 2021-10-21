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
import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule._
import fr.linkit.api.gnom.cache.sync.behavior.annotation.{MethodControl, Synchronized}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.engine.gnom.cache.SharedCacheDistantManager
import fr.linkit.engine.gnom.network.NetworkDataTrunk.CacheManagerInfo

import java.sql.Timestamp
import scala.collection.mutable

//FIXME OriginManagers and DistantManagers
class NetworkDataTrunk private(network: AbstractNetwork, val startUpDate: Timestamp) {

    private val traffic = network.connection.traffic
    private val engines = mutable.HashMap.empty[String, Engine]
    private val caches  = mutable.HashMap.empty[String, (SharedCacheManager, Array[Int])]

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

    @MethodControl(value = BROADCAST, innerInvocations = true)
    def newEngine(engineIdentifier: String): Engine = {
        if (engines.contains(engineIdentifier))
            throw new IllegalArgumentException("This engine already exists !")
        val current      = ExecutorEngine.currentEngine
        val cacheManager = {
            if (network.connectionEngine ne current) getDistantCache(engineIdentifier)
            else network.newCacheManager(engineIdentifier)._1
        }
        val engine       = new DefaultEngine(engineIdentifier, cacheManager)
        addEngine(engine)
    }

    @MethodControl(value = BROADCAST_IF_ROOT_OWNER)
    def removeEngine(engine: Engine): Unit = engines -= engine.identifier

    def findCache(family: String): Option[SharedCacheManager] = caches.get(family).map(_._1)

    @MethodControl(value = BROADCAST)
    def addCacheManager(manager: SharedCacheManager, storePath: Array[Int]): Unit = {
        if (caches.contains(manager.family))
            throw new Exception("Cache Manager already exists")
        caches.put(manager.family, (manager, storePath))
    }

    def findEngine(identifier: String): Option[Engine] = engines.get(identifier)

    def listEngines: List[Engine] = engines.values.toList

    def countConnection: Int = engines.size

    protected def addEngine(@Synchronized engine: Engine): Engine = {
        engines.put(engine.identifier, engine)
        engine
    }

    private def getDistantCache(engineIdentifier: String): SharedCacheManager = caches.getOrElseUpdate(engineIdentifier, {
        val code  = engineIdentifier.hashCode
        val store = traffic.createStore(code)
        (new SharedCacheDistantManager(engineIdentifier, engineIdentifier, network, store), store.trafficPath)
    })._1

}

object NetworkDataTrunk {

    case class CacheManagerInfo(family: String, owner: String, storePath: Array[Int])

    def fromData(data: NetworkDataBundle): NetworkDataTrunk = {
        val network = data.network
        val trunk   = new NetworkDataTrunk(network)
        data.engines.foreach { identifier =>
            trunk.addEngine(new DefaultEngine(identifier, trunk.getDistantCache(identifier)))
        }
        data.caches.foreach(info => {
            import info._
            trunk.findCache(family).getOrElse {
                val store = getStore(network.connection, storePath)
                val cache = new SharedCacheDistantManager(family, owner, network, store)
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
