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

package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{Network, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope
import fr.`override`.linkit.core.connection.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel


abstract class AbstractNetwork(override val connection: ConnectionContext,
                               override val globalCache: SharedCacheManager) extends Network {

    protected val sharedIdentifiers: SharedCollection[String] = globalCache
            .get(3, SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    protected val communicator: CommunicationPacketChannel =
        connection.getInjectable(9, ChannelScope.broadcast, CommunicationPacketChannel.providable)

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    override def isConnected(identifier: String): Boolean = getEntity(identifier).isDefined

    protected def createEntity(identifier: String): NetworkEntity = {
        if (identifier == connection.supportIdentifier) {
            return connectionEntity
        }

        println(s"Creating entity $identifier")
        val channel = communicator.subInjectable(Array(identifier), CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

}

