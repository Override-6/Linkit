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

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.cache.{CacheSearchBehavior, SharedCacheManager}
import fr.linkit.api.connection.network.{ExternalConnectionState, Network}
import fr.linkit.engine.connection.network.AbstractRemoteEngine
import fr.linkit.engine.connection.cache.SharedInstance

class ConnectionEngine private[network](connection: ConnectionContext,
                                        identifier: String,
                                        cache: SharedCacheManager)
        extends AbstractRemoteEngine(identifier, cache) {

    override val network: Network = connection.network

    private val stateInstance = cache.attachToCache[SharedInstance[ExternalConnectionState]](3, SharedInstance[ExternalConnectionState], CacheSearchBehavior.GET_OR_WAIT)

    override def getConnectionState: ExternalConnectionState = stateInstance.get.get

    //TODO Client-side Events
    stateInstance.addListener(newState => {
        //TODO Client-side Events
    })
}
