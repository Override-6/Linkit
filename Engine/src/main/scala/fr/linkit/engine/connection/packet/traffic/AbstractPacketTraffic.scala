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

package fr.linkit.engine.connection.packet.traffic

import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.{ClosedException, Reason}
import fr.linkit.engine.connection.packet.SimplePacketBundle

import scala.reflect.ClassTag

abstract class AbstractPacketTraffic(override val currentIdentifier: String, procrastinator: ProcrastinatorControl) extends PacketTraffic {

    @volatile private var closed   = false
    override  val path: Array[Int] = Array.empty
    protected val rootStore        = new SimplePacketInjectableStore(this, path)

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C = {
        rootStore.getInjectable[C](injectableID, factory, scopeFactory)
    }

    override def close(reason: Reason): Unit = {
        rootStore.close()
        closed = true
    }

    override def isClosed: Boolean = closed

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    override def processInjection(packet: Packet, attr: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit = {
        processInjection(SimplePacketBundle(packet, attr, coordinates))
    }

    override def processInjection(bundle: PacketBundle): Unit = procrastinator.runLater {
        rootStore.inject(new SimplePacketInjection(bundle))
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = rootStore.findStore(id)

    override def createStore(id: Int): PacketInjectableStore = rootStore.createStore(id)
}