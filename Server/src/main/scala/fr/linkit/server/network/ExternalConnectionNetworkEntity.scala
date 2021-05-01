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

package fr.linkit.server.network

import fr.linkit.api.connection.network.cache.{CacheOpenBehavior, SharedCacheManager}
import fr.linkit.api.connection.network.{ExternalConnectionState, Network}
import fr.linkit.engine.connection.network.AbstractRemoteEntity
import fr.linkit.engine.connection.network.cache.SharedInstance
import fr.linkit.server.connection.ServerConnection

class ExternalConnectionNetworkEntity private[network](serverConnection: ServerConnection,
                                                       identifier: String,
                                                       entityCache: SharedCacheManager)
        extends AbstractRemoteEntity(identifier, entityCache) {

    override val network: Network = serverConnection.network
    private  val connection       = serverConnection.getConnection(identifier).get

    entityCache.getCache(3, SharedInstance[ExternalConnectionState], CacheOpenBehavior.GET_OR_WAIT)
            .set(ExternalConnectionState.CONNECTED) //technically already connected

    override def getConnectionState: ExternalConnectionState = connection.getState

    // connection.addConnectionStateListener(state => server.runLater(sharedState.set(state)))
}

