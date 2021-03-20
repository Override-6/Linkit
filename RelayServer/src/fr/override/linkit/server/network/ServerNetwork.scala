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

import fr.`override`.linkit.api.network.cache.SharedInstance
import fr.`override`.linkit.api.network.cache.collection.BoundedCollection
import fr.`override`.linkit.api.network.{AbstractNetwork, ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.server.RelayServer

import java.sql.Timestamp

class ServerNetwork(server: RelayServer)(implicit traffic: PacketTraffic) extends AbstractNetwork(server) {

    override val startUpDate: Timestamp = globalCache.post(2, new Timestamp(System.currentTimeMillis()))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .add(server.identifier)
                .flush()
                .mapped(createEntity)
    }
    selfEntity
            .cache
            .get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically already connected

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(server, identifier, communicator)
    }

    def removeEntity(identifier: String): Unit = {
        getEntity(identifier)
                .foreach(entity => {
                    if (entity.getConnectionState != ConnectionState.CLOSED)
                        throw new IllegalStateException(s"Could not remove entity '$identifier' from network as long as it still open")
                    sharedIdentifiers.remove(identifier)
                })
    }
}
