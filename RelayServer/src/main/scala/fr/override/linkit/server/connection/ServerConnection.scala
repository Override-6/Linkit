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
import fr.`override`.linkit.api.connection.network.{ConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier

import scala.reflect.ClassTag

class ServerConnection extends ConnectionContext {
    override val configuration: ConnectionConfiguration = _
    override val boundIdentifier: String = _

    override def traffic: PacketTraffic = ???

    override def network: Network = ???

    override def translator: PacketTranslator = ???

    override val eventNotifier: EventNotifier = _

    override def getState: ConnectionState = ???

    override def shutdown(): Unit = ???

    override def isAlive(): Boolean = ???

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = ???

    override def runLater(task: => Unit): Unit = ???

    def getConnection(identifier: String): ConnectionContext = ???
}
