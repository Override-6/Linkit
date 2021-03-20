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

package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedInstance
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel

class RelayNetworkEntity(relay: Relay, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(relay, identifier, communicator) {

    private val stateInstance = cache.get(3, SharedInstance[ConnectionState])

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = stateInstance.addListener(action)

    override def getConnectionState: ConnectionState = stateInstance.get
}
