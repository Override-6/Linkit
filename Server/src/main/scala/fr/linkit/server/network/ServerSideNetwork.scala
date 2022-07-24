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

package fr.linkit.server.network

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.engine.gnom.cache.SharedCacheOriginManager
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.New
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.internal.language.bhv.{Contract, ObjectsProperty}

class ServerSideNetwork(traffic: AbstractPacketTraffic)
        extends AbstractNetwork(traffic) {


    override def serverIdentifier: String = traffic.currentIdentifier

    override protected def retrieveDataTrunk(): NetworkDataTrunk = {
        val contracts = Contract("NetworkContract", ObjectsProperty.defaults(this))
        globalCaches.attachToCache(0, DefaultConnectedObjectCache[NetworkDataTrunk](this))
                .syncObject(0, New[NetworkDataTrunk](this), contracts)
    }

    override protected def createGlobalCache: SharedCacheManager = {
        new SharedCacheOriginManager(GlobalCacheID, this, networkStore.createStore(GlobalCacheID.hashCode))
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