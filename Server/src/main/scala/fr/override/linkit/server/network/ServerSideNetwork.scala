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
import fr.`override`.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.core.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.core.connection.network.cache.{AbstractSharedCacheManager, SharedInstance}
import fr.`override`.linkit.core.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.server.connection.{ServerConnection, ServerExternalConnection}
import fr.`override`.linkit.server.network.ServerSideNetwork.defaultCache

import java.sql.Timestamp

class ServerSideNetwork private(serverConnection: ServerConnection,
                                globalCache: SharedCacheManager)(implicit traffic: PacketTraffic)
        extends AbstractNetwork(serverConnection, globalCache) {

    def this(server: ServerConnection, traffic: PacketTraffic) = {
        this(server, defaultCache(server, traffic))(traffic)
    }

    override val serverIdentifier: String = serverConnection.supportIdentifier

    override val connectionEntity: NetworkEntity = {
        val selfCache = AbstractSharedCacheManager.get(serverIdentifier, serverIdentifier, ServerSharedCacheManager())
        new SelfNetworkEntity(serverConnection, ExternalConnectionState.CONNECTED, selfCache) //Server is always connected to... server !
    }

    //The current connection is the network's server connection.
    override def serverEntity: NetworkEntity = connectionEntity

    override val startUpDate: Timestamp = globalCache.post(2, new Timestamp(System.currentTimeMillis()))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                //.addListener((_, _, _) => if (entities != null) println("entities are now : " + entities)) //debug purposes
                .add(serverIdentifier)
                .flush()
                .mapped(createEntity)
                .addListener(handleTraffic)
    }
    connectionEntity
            .cache
            .get(3, SharedInstance[ExternalConnectionState])
            .set(ExternalConnectionState.CONNECTED) //technically always connected

    override protected def createEntity(identifier: String): NetworkEntity = {
        //TODO Create remote breakpoints
        //FIXME Could return an unexpected if another packet (ex: setProperty) is received into this communicator.
        super.createEntity(identifier)
    }

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ExternalConnectionNetworkEntity(serverConnection, identifier)
    }

    def removeEntity(identifier: String): Unit = {
        getEntity(identifier)
                .foreach(entity => {
                    if (entity.getConnectionState != ExternalConnectionState.CLOSED)
                        throw new IllegalStateException(s"Could not remove entity '$identifier' from network as long as it still open")
                    sharedIdentifiers.remove(identifier)
                })
    }

    private[server] def addEntity(connection: ServerExternalConnection): Unit = {
        sharedIdentifiers.add(connection.supportIdentifier)
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        /*lazy val entity = entityOpt.orNull //get
        println(s"mod = ${mod}")
        println(s"index = ${index}")
        println(s"entity = ${entity}")

        import CollectionModification._
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        //server.eventNotifier.notifyEvent(server.networkHooks, event)
        */
    }
}

object ServerSideNetwork {
    private def defaultCache(serverConnection: ServerConnection, traffic: PacketTraffic): SharedCacheManager = {
        AbstractSharedCacheManager.get("Global Shared Cache", serverConnection.supportIdentifier, ServerSharedCacheManager())(traffic)
    }
}
