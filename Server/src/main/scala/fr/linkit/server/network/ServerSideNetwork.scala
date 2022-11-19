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

package fr.linkit.server.network

import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.api.gnom.cache.sync.instantiation.New
import fr.linkit.api.gnom.cache.{CacheManagerAlreadyDeclaredException, SharedCacheManager}
import fr.linkit.api.gnom.network.tag.{NameTag, NetworkFriendlyEngineTag, Server, UniqueTag}
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}
import fr.linkit.server.cache.ServerSharedCacheManager
import fr.linkit.server.connection.ServerConnection

class ServerSideNetwork(connection: ServerConnection)
        extends AbstractNetwork(connection) {


    override protected def listInitialNTs: List[NameTag] = List(currentTag)

    override protected def createNewCache0(family: String, managerChannelPath: Array[Int]): SharedCacheManager = {
        new ServerSharedCacheManager(family, this, traffic.getObjectManagementChannel, getStore(managerChannelPath))
    }

    override def retrieveNT(uniqueTag: UniqueTag with NetworkFriendlyEngineTag): NameTag = uniqueTag match {
        //add Server case which (super.retrieveNT would also support the Server case but this override supports Server case during trunk initialization)
        case Server => currentTag
        case _      => super.retrieveNT(uniqueTag)
    }

    def attachToCacheManager(family: String): SharedCacheManager = {
        findCacheManager(family).getOrElse(declareNewCacheManager(family))
    }

    def declareNewCacheManager(family: String): SharedCacheManager = {
        if (isTrunkInitializing)
            throw new UnsupportedOperationException("Trunk is initializing.")
        if (trunk.findCacheManager(family).isDefined)
            throw new CacheManagerAlreadyDeclaredException(s"Cache of family $family is already opened.")
        newCacheManager(family)
    }

    private[network] def newCacheManager(family: String): SharedCacheManager = {
        val store       = networkStore.createStore(family.hashCode)
        val manager     = new ServerSharedCacheManager(family, this, traffic.getObjectManagementChannel, store)
        val trafficPath = store.trafficPath
        registerSCMReference(manager.reference)
        addCacheManager(manager, trafficPath)
        manager
    }


    override protected def retrieveDataTrunk(): NetworkDataTrunk = {
        val contracts = Contract("NetworkContract", ObjectsProperty.defaults(this))
        globalCaches.attachToCache(0, DefaultConnectedObjectCache[NetworkDataTrunk](this))
                .syncObject(0, New[NetworkDataTrunk](this), contracts)
    }

    def removeEngine(identifier: NameTag): Unit = {
        getEngine(identifier).foreach(trunk.removeEngine)
    }

    override def initialize(): Unit = {
        super.initialize()
        declareNewCacheManager("StaticAccesses")
        addCacheManager(globalCaches, networkStore.trafficPath :+ globalCaches.family.hashCode)
    }
}