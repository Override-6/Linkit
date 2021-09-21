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
import fr.linkit.api.connection.cache.SharedCacheManager
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule._
import fr.linkit.api.connection.cache.obj.behavior.annotation.{MethodControl, Synchronized}
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.api.connection.packet.traffic.PacketInjectableStore
import fr.linkit.engine.connection.cache.SharedCacheDistantManager
import fr.linkit.engine.connection.cache.obj.invokation.ExecutorEngine
import fr.linkit.engine.connection.network.NetworkDataTrunk.CacheManagerInfo

import scala.collection.mutable

//FIXME OriginManagers and DistantManagers
class NetworkDataTrunk private(network: Network, val startUpDate: Timestamp) {

    private val engines = mutable.HashMap.empty[String, Engine]
    private val caches  = mutable.HashMap.empty[String, SharedCacheManager]

    def this(network: Network) {
        this(network, new Timestamp(System.currentTimeMillis()))
    }

    def toBundle: NetworkDataBundle = NetworkDataBundle(engines.keys.toArray, caches.map(p => toCacheData(p._2)).toArray, startUpDate, network)

    private def toCacheData(manager: SharedCacheManager): CacheManagerInfo = {
        CacheManagerInfo(manager.family, manager.ownerID, manager.trafficPath)
    }

    @MethodControl(value = BROADCAST, innerInvocations = true)
    def newEngine(engineIdentifier: String): Engine = {
        if (engines.contains(engineIdentifier))
            throw new IllegalArgumentException("This engine already exists !")
        val current      = ExecutorEngine.currentEngine
        val cacheManager = {
            if (network.connectionEngine ne current) caches.getOrElseUpdate(engineIdentifier, {
                val code  = engineIdentifier.hashCode
                val store = network.connection.createStore(code)
                new SharedCacheDistantManager(engineIdentifier, engineIdentifier, network, store)
            })
            else network.declareNewCacheManager(engineIdentifier)
        }
        val engine       = new DefaultEngine(engineIdentifier, cacheManager)
        engines.put(engineIdentifier, engine)
        engine
    }

    @MethodControl(value = BROADCAST_IF_ROOT_OWNER)
    def removeEngine(engine: Engine): Unit = engines -= engine.identifier

    def findCache(family: String): Option[SharedCacheManager] = caches.get(family)

    @MethodControl(value = BROADCAST)
    def addCacheManager(manager: SharedCacheManager): Unit = {
        if (caches.contains(manager.family))
            throw new Exception("Cache Manager already exists")
        caches.put(manager.family, manager)
    }

    def findEngine(identifier: String): Option[Engine] = engines.get(identifier)

    def listEngines: List[Engine] = engines.values.toList

    def countConnection: Int = engines.size

    protected def addEngine(@Synchronized engine: Engine): Engine = {
        engines.put(engine.identifier, engine)
        engine
    }


}

object NetworkDataTrunk {

    case class CacheManagerInfo(family: String, owner: String, storePath: Array[Int])

    def fromData(data: NetworkDataBundle): NetworkDataTrunk = {
        val network = data.network
        val trunk   = new NetworkDataTrunk(network)
        data.engines.foreach(trunk.newEngine)
        data.caches.foreach(info => {
            import info._
            trunk.findCache(family).getOrElse {
                val store = getStore(network.connection, storePath)
                val cache = new SharedCacheDistantManager(family, owner, network, store)
                trunk.addCacheManager(cache)
            }
        })
        trunk
    }

    private def getStore(connection: ConnectionContext, path: Array[Int]): PacketInjectableStore = {
        var store: PacketInjectableStore = connection
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
