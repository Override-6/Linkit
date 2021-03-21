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

package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.Network
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.core.local.system.ContextLogger
import fr.`override`.linkit.core.local.system.event.DefaultEventNotifier
import fr.`override`.linkit.server.config.ServerConnectionConfiguration
import fr.`override`.linkit.server.network.ServerNetwork
import fr.`override`.linkit.server.{ServerApplicationContext, ServerPacketTraffic}

import scala.reflect.ClassTag

class ServerConnection(applicationContext: ServerApplicationContext,
                       override val configuration: ServerConnectionConfiguration) extends ConnectionContext {
    override val supportIdentifier: String = configuration.identifier
    override val traffic: PacketTraffic = new ServerPacketTraffic(this)
    override val network: Network = new ServerNetwork(this, traffic)
    override val eventNotifier: EventNotifier = new DefaultEventNotifier

    private val connectionsManager: ExternalConnectionsManager = new ExternalConnectionsManager(this)
    @volatile private var alive = true

    override def shutdown(): Unit = {
        if (!alive)
            throw new IllegalStateException("Server is already shutdown !")
        val port = configuration.port

        ContextLogger.info(s"Server '$supportIdentifier' on port '$port' prepares to shutdown...")
        applicationContext.unregister(this)

        connectionsManager.close()
        alive = false

    }

    override def isAlive: Boolean = alive

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.get
    }

    override def runLater(task: => Unit): Unit = ???

    def getConnection(identifier: String): ConnectionContext = ???

}
