/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{ExternalConnectionState, Network}
import fr.`override`.linkit.core.connection.network.AbstractRemoteEntity
import fr.`override`.linkit.core.connection.network.cache.{AbstractSharedCacheManager, SharedInstance}
import fr.`override`.linkit.server.connection.ServerConnection

class ExternalConnectionNetworkEntity private(serverConnection: ServerConnection,
                                              identifier: String,
                                              cache: SharedCacheManager)
        extends AbstractRemoteEntity(identifier, cache) {

    def this(server: ServerConnection, identifier: String) = {
        this(server, identifier, AbstractSharedCacheManager.get(identifier, identifier, ServerSharedCacheManager())(server.traffic))
    }

    private val connection = serverConnection.getConnection(identifier).get
    cache.get(3, SharedInstance[ExternalConnectionState])
            .set(ExternalConnectionState.CONNECTED) //technically already connected

    override def getConnectionState: ExternalConnectionState = connection.getState

    override val network: Network = serverConnection.network
    // connection.addConnectionStateListener(state => server.runLater(sharedState.set(state)))
}
