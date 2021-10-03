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

package fr.linkit.client.network

import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.{CacheSearchBehavior, SharedCacheManager}
import fr.linkit.api.gnom.network.NetworkInitialisable
import fr.linkit.api.gnom.reference.MutableReferencedObjectStore
import fr.linkit.client.connection.ClientConnection
import fr.linkit.engine.gnom.cache.SharedCacheDistantManager
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCenter
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}

class ClientSideNetwork(connection: ClientConnection,
                        refStore: MutableReferencedObjectStore,
                        privilegedInitialisables: Array[NetworkInitialisable]) extends AbstractNetwork(connection, refStore, privilegedInitialisables) {

    override protected def retrieveDataTrunk(store: ObjectBehaviorStore): NetworkDataTrunk = {
        val trunk = globalCache.attachToCache(0, DefaultSynchronizedObjectCenter[NetworkDataTrunk](store, this), CacheSearchBehavior.GET_OR_CRASH)
                .findObject(0)
                .getOrElse {
                    throw new NoSuchElementException("Engine Store not found.")
                }
        trunk
    }

    override protected def createGlobalCache: SharedCacheManager = {
        new SharedCacheDistantManager(GlobalCacheID, serverIdentifier, this, networkStore.createStore(GlobalCacheID.hashCode))
    }

    override def serverIdentifier: String = connection.boundIdentifier

}