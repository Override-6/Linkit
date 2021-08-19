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

package fr.linkit.client.connection.network

import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehaviorStore
import fr.linkit.api.connection.cache.{CacheSearchBehavior, NoSuchCacheException, SharedCacheManager}
import fr.linkit.api.connection.network.Updatable
import fr.linkit.client.connection.ClientConnection
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter
import fr.linkit.engine.connection.network.{AbstractNetwork, NetworkDataTrunk}

class ClientSideNetwork(connection: ClientConnection) extends AbstractNetwork(connection) {

    override protected def retrieveEngineStore(store: SynchronizedObjectBehaviorStore): NetworkDataTrunk = {
        globalCache.attachToCache(0, DefaultSynchronizedObjectCenter[NetworkDataTrunk](store), CacheSearchBehavior.GET_OR_CRASH)
                .findObject(0)
                .getOrElse {
                    throw new NoSuchElementException("Engine Store not found.")
                }
    }

    override protected def createGlobalCache: SharedCacheManager = {
        findDistantCacheManager("Global Cache", serverIdentifier).getOrElse {
            throw new NoSuchCacheException("Could not find Global Cache manager")
        }
    }

    override def serverIdentifier: String = connection.boundIdentifier

}