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
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.network.NetworkInitialisable
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.engine.gnom.cache.SharedCacheOriginManager
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCenter
import fr.linkit.engine.gnom.cache.sync.instantiation.SyncConstructor
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.server.connection.ServerConnection

class ServerSideNetwork(serverConnection: ServerConnection,
                        privilegedInitialisables: Array[NetworkInitialisable])(implicit traffic: AbstractPacketTraffic)
        extends AbstractNetwork(serverConnection, traffic.defaultPersistenceConfig.getReferenceStore, privilegedInitialisables) {

    trunk.addCacheManager(globalCache)

    override def serverIdentifier: String = serverConnection.currentIdentifier

    override protected def retrieveDataTrunk(store: ObjectBehaviorStore): NetworkDataTrunk = {
        globalCache.attachToCache(0, DefaultSynchronizedObjectCenter[NetworkDataTrunk](this))
                .syncObject(0, SyncConstructor[NetworkDataTrunk](this), store)
    }

    override protected def createGlobalCache: SharedCacheManager = {
        new SharedCacheOriginManager(GlobalCacheID, this, networkStore.createStore(GlobalCacheID.hashCode))
    }

    def removeEngine(identifier: String): Unit = {
        findEngine(identifier).foreach(trunk.removeEngine)
    }
}
