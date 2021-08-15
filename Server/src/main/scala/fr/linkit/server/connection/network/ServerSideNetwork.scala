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

package fr.linkit.server.connection.network

import fr.linkit.api.connection.cache.{CacheSearchBehavior, NoSuchCacheException, SharedCacheManager}
import fr.linkit.api.connection.network.{Engine, ExternalConnectionState}
import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.engine.connection.cache.SharedInstance
import fr.linkit.engine.connection.cache.collection.BoundedCollection
import fr.linkit.engine.connection.network.{AbstractNetwork, SelfEngine}
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.server.connection.ServerConnection

import java.sql.Timestamp

class ServerSideNetwork(serverConnection: ServerConnection)(implicit traffic: PacketTraffic)
        extends AbstractNetwork(serverConnection) {

    override lazy      val globalCache     : SharedCacheManager                  = declareNewCacheManager("Global Cache")
    override           val connectionEngine: Engine                              = createServerEntity()
    override protected val entities        : BoundedCollection.Immutable[Engine] = {
        sharedIdentifiers
                .add(serverIdentifier)
                .mapped(createEntity)
    }

    override val startUpDate: Timestamp = new Timestamp(0) //cache.postInstance(2, new Timestamp(System.currentTimeMillis()))

    override def serverIdentifier: String = serverConnection.currentIdentifier

    //The current connection is the network's server connection.
    override def serverEngine: Engine = connectionEngine

    override def createEngine(identifier: String, communicator: SyncAsyncPacketChannel): Engine = {
        val entityCache = findDistantCacheManager(identifier, identifier).getOrElse {
            throw new NoSuchCacheException(s"No cache manager is set for engine $identifier")
        }
        new ExternalConnectionEngine(serverConnection, identifier, entityCache)
    }

    def removeEntity(identifier: String): Unit = {
        getEngine(identifier)
                .foreach(entity => {
                    if (entity.getConnectionState != ExternalConnectionState.CLOSED)
                        throw new IllegalStateException(s"Could not remove entity '$identifier' from network as long as it still open")
                    sharedIdentifiers.remove(identifier)
                })
    }

    def createServerEntity(): Engine = {
        val selfCache    = declareNewCacheManager(serverIdentifier)
        val serverEntity = new SelfEngine(serverConnection, ExternalConnectionState.CONNECTED, this, selfCache) //Server always connected to himself
        serverEntity
                .cache
                .attachToCache(3, SharedInstance[ExternalConnectionState], CacheSearchBehavior.GET_OR_WAIT)
                .set(ExternalConnectionState.CONNECTED) //technically always connected
        serverEntity
    }
}
