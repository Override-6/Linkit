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

import fr.`override`.linkit.api.network.cache.{SharedCacheHandler, SharedInstance}
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.server.RelayServer

class ConnectionNetworkEntity private(server: RelayServer,
                                      identifier: String,
                                      cacheHandler: SharedCacheHandler,
                                      communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(server, identifier, cacheHandler, communicator) {

    def this(server: RelayServer, identifier: String, communicator: CommunicationPacketChannel) = {
        this(server, identifier, SharedCacheHandler.get(identifier, ServerSharedCacheHandler())(server.traffic), communicator)
    }

    private val connection = server.getConnection(identifier).get
    private val sharedState = cache.get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically already connected

    override def getConnectionState: ConnectionState = connection.getState
    connection.addConnectionStateListener(state => server.runLater(sharedState.set(state)))

}
