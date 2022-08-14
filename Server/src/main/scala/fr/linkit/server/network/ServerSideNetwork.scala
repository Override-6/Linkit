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

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.engine.gnom.cache.SharedCacheOriginManager
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.New
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.internal.language.bhv.ContractImpl

class ServerSideNetwork(traffic: AbstractPacketTraffic)
        extends AbstractNetwork(traffic) {


    override def serverIdentifier: String = traffic.currentIdentifier

    override protected def retrieveDataTrunk(): NetworkDataTrunk = {
        val contracts = ContractImpl("NetworkContract", ObjectsProperty.defaults(this))
        globalCaches.attachToCache(0, DefaultConnectedObjectCache[NetworkDataTrunk](this))
                .syncObject(0, New[NetworkDataTrunk](this), contracts)
    }

    override protected def createGlobalCache: SharedCacheManager = {
        new SharedCacheOriginManager(GlobalCacheID, this, objectManagementChannel, networkStore.createStore(GlobalCacheID.hashCode))
    }

    def removeEngine(identifier: String): Unit = {
        findEngine(identifier).foreach(trunk.removeEngine)
    }
    
    override def initialize(): this.type = {
        super.initialize()
        addCacheManager(globalCaches, networkStore.trafficPath :+ globalCaches.family.hashCode)
        this
    }
}