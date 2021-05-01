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

package fr.linkit.client.network

import fr.linkit.api.connection.network.cache.CacheOpenBehavior
import fr.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.client.connection.ClientConnection
import fr.linkit.engine.connection.network.cache.SharedInstance
import fr.linkit.engine.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.linkit.engine.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel

import java.sql.Timestamp

class ClientSideNetwork(connection: ClientConnection) extends AbstractNetwork(connection) {

    override val connectionEntity: SelfNetworkEntity = initSelfEntity

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) AppLogger.vDebug("entities are now : " + entities)) //debug purposes
                .mapped(createEntity)
                .addListener(handleTraffic)
    }

    override def serverIdentifier: String = connection.boundIdentifier

    override def serverEntity: NetworkEntity = getEntity(serverIdentifier).get

    override def startUpDate: Timestamp = globalCache(2)

    override def createEntity0(identifier: String, communicator: SyncAsyncPacketChannel): NetworkEntity = {
        val entityCache = newCachesManager(identifier, identifier)
        new ConnectionNetworkEntity(connection, identifier, entityCache)
    }

    def update(): Unit = {
        globalCache.update()
        connectionEntity.update()
    }

    def initSelfEntity: SelfNetworkEntity = {
        val identifier   = connection.supportIdentifier
        val sharedCaches = newCachesManager(identifier, identifier)
        sharedCaches
                .getCache(3, SharedInstance[ExternalConnectionState], CacheOpenBehavior.GET_OR_WAIT)
                .set(ExternalConnectionState.CONNECTED) //technically always connected to himself
        new SelfNetworkEntity(connection, connection.getState, sharedCaches)
    }

    private[client] def handshake(): Unit = {
        //AppLogger.debug("HANDSHAKING...")
        val identifier = connection.supportIdentifier
        if (!sharedIdentifiers.contains(identifier))
            sharedIdentifiers.add(identifier)
        //AppLogger.debug("HANDSHAKE MADE !")
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