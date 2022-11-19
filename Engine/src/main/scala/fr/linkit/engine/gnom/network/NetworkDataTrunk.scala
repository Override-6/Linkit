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

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.network.tag.{IdentifierTag, NameTag, NetworkFriendlyEngineTag}
import fr.linkit.engine.gnom.network.NetworkDataTrunk.{CacheManagerInfo, NetworkDataBundle}
import fr.linkit.engine.gnom.network.statics.StaticAccesses
import fr.linkit.engine.internal.util.ConsumerContainer

import java.sql.Timestamp
import scala.collection.mutable

class NetworkDataTrunk private(network: AbstractNetwork, val startUpDate: Timestamp) {


    private               val engines           = mutable.HashMap.empty[String, EngineImpl]
    private               val cacheManagers     = mutable.HashMap.empty[String, (SharedCacheManager, Array[Int])]
    private[network] lazy val staticAccesses    = new StaticAccesses(network)
    private               val onNewEngineEvents = ConsumerContainer[Engine]()

    def this(network: AbstractNetwork) {
        this(network, new Timestamp(System.currentTimeMillis()))
    }

    def toBundle: NetworkDataBundle = {
        NetworkDataBundle(engines.keys.toArray, cacheManagers.map(p => toCacheData(p._2)).toArray, startUpDate, network)
    }

    private def toCacheData(pair: (SharedCacheManager, Array[Int])): CacheManagerInfo = {
        val (manager: SharedCacheManager, storePath: Array[Int]) = pair
        CacheManagerInfo(manager.family, storePath)
    }

    def newEngine(engineIdentifier: String): Engine = {
        if (engines.contains(engineIdentifier))
            throw new IllegalArgumentException("This engine already exists !")
        addEngine(new EngineImpl(engineIdentifier, network))
    }

    def removeEngine(engine: Engine): Unit = engines -= engine.name

    def findCacheManager(family: String): Option[SharedCacheManager] = {
        cacheManagers.get(family).map(_._1)
    }

    /**
     * add manager into this trunk.
     *
     * @param manager     shared cache manager to store
     * @param channelPath the traffic path of the manager's channel.
     * */
    private[network] def addCacheManager(manager: SharedCacheManager, channelPath: Array[Int]): Unit = {
        if (cacheManagers.contains(manager.family))
            throw new Exception(s"Cache Manager '${manager.family}' already exists")
        cacheManagers.put(manager.family, (manager, channelPath))
    }

    def findEngine(identifier: IdentifierTag): Option[Engine] = engines.synchronized {
        engines.values.find(_.isIncluded(identifier))
    }

    def findEngine(name: NameTag): Option[Engine] = engines.synchronized {
        engines.get(name.name)
    }

    def listEngines(group: NetworkFriendlyEngineTag): List[Engine] = engines.synchronized {
        engines.values.filter(_.isIncluded(group)).toList
    }

    def listEngines: List[Engine] = engines.synchronized(engines.values.toList)

    def countConnection: Int = engines.size

    protected def addEngine(engine: EngineImpl): EngineImpl = engine.synchronized {
        engines.put(engine.name, engine) match {
            case None    => onNewEngineEvents.applyAll(engine)
            case Some(_) =>
        }
        engine.classMappings // init mappings
        engine
    }

    //When the trunk gets deserialized, due to the TypePersistence that will deserialize the trunk,
    // the engines in it will not come in their connected version,
    // so for each engines already present on the object's creation,
    //we add them in order to apply the NetworkContract.bhv that will synchronize the new engines

    def reinjectEngines(): this.type = {
        if (engines.nonEmpty) engines.values.foreach(e => {
            addEngine(e)
        })
        this
    }

    private[network] def onNewEngine(f: Engine => Unit): Unit = {
        onNewEngineEvents += f
    }

}

object NetworkDataTrunk {

    case class CacheManagerInfo(family: String, managerChannelPath: Array[Int])

    case class NetworkDataBundle(engines: Array[String], caches: Array[CacheManagerInfo], startUpDate: Timestamp, network: AbstractNetwork)

    def fromData(data: NetworkDataBundle): NetworkDataTrunk = {
        val network = data.network
        val trunk   = new NetworkDataTrunk(network)
        data.engines.foreach { identifier =>
            trunk.addEngine(new EngineImpl(identifier, network))
        }
        data.caches.foreach(info => {
            import info._
            if (trunk.findCacheManager(family).isEmpty)
                trunk.findCacheManager(family).getOrElse {
                    val cache = network.createNewCache(family, managerChannelPath)
                    trunk.addCacheManager(cache, managerChannelPath)
                }
        })
        trunk
    }


}
