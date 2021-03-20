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

package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.{CommunicationPacketChannel, PacketChannelCategories}
import fr.`override`.linkit.api.system.Version

import java.sql.Timestamp

class SelfNetworkEntity(relay: Relay, override val cache: SharedCacheHandler) extends NetworkEntity {

    override val identifier: String = relay.identifier

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    override val apiVersion: Version = Relay.ApiVersion

    override val relayVersion: Version = relay.relayVersion
    update()

    override def update(): this.type = {
        cache.post(2, connectionDate)
        cache.post(4, apiVersion)
        cache.post(5, relayVersion)
        cache.update()
        this
    }

    override def getConnectionState: ConnectionState = relay.getConnectionState

    override def getProperty(name: String): Serializable = relay.properties.get(name).orNull

    override def setProperty(name: String, value: Serializable): Unit = relay.properties.putProperty(name, value)

    override def getRemoteConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        val communicator = relay
                .getInjectable(4, ChannelScope.broadcast, PacketChannelCategories)
                .subInjectable(identifier, CommunicationPacketChannel.providable, true)

        val fragmentHandler = relay.extensionLoader.fragmentHandler
        fragmentHandler
                .listRemoteFragments()
                .map(frag => new RemoteFragmentController(frag.nameIdentifier, communicator))
    }

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        listRemoteFragmentControllers.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"SelfNetworkEntity(identifier: ${relay.identifier})"

}
