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

package fr.linkit.core.connection.network

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.network.{ExternalConnectionState, Network, NetworkEntity}
import fr.linkit.api.local.system.AppLogger

import java.sql.Timestamp

class SelfNetworkEntity(connection: ConnectionContext,
                        state: => ExternalConnectionState,
                        override val entityCache: SharedCacheManager) extends NetworkEntity {

    override val identifier: String = connection.supportIdentifier

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    override val network: Network = connection.network

    //override val apiVersion: Version = Relay.ApiVersion

    //override val relayVersion: Version = relay.relayVersion

    update()

    override def update(): this.type = {
        entityCache.postInstance(2, connectionDate)
        //cache.post(4, apiVersion)
        //cache.post(5, relayVersion)
        entityCache.update()
        this
    }

    override def getConnectionState: ExternalConnectionState = state

    override def toString: String = s"SelfNetworkEntity(identifier: ${identifier})"

}
