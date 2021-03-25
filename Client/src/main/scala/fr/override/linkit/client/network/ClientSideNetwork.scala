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

import fr.`override`.linkit.api.connection.network.NetworkEntity
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.client.ClientConnection
import fr.`override`.linkit.core.connection.network.cache.SimpleSharedCacheManager
import fr.`override`.linkit.core.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.core.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel

import java.sql.Timestamp

class ClientSideNetwork(connection: ClientConnection, globalCache: SharedCacheManager) extends AbstractNetwork(connection, globalCache) {
    private implicit val traffic: PacketTraffic = connection.traffic

    override val serverIdentifier: String = connection.boundIdentifier

    override def serverEntity: NetworkEntity = getEntity(serverIdentifier).get

    override val connectionEntity: SelfNetworkEntity = initDefaultEntity

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .mapped(createEntity)
                .addListener(handleTraffic)
    }

    override def startUpDate: Timestamp = globalCache(2)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(connection, identifier, communicator)
    }

    def update(): Unit = {
        globalCache.update()
        connectionEntity.update()
    }

    def initDefaultEntity: SelfNetworkEntity = {
        val identifier = connection.supportIdentifier
        val sharedCache = SimpleSharedCacheManager.get(identifier, identifier)
        new SelfNetworkEntity(connection, connection.getState, sharedCache)
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        //lazy val entity = entityOpt.get
        /*val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        relay.eventNotifier.notifyEvent(relay.networkHooks, event)
         */
    }

}