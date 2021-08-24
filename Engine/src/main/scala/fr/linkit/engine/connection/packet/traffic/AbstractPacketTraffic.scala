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
import fr.linkit.api.connection.packet.traffic.injection.PacketInjectionController
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.{AppLogger, ClosedException, Reason}
import fr.linkit.engine.connection.packet.SimplePacketBundle
import fr.linkit.engine.connection.packet.traffic.injection.ParallelInjectionContainer

import scala.reflect.ClassTag

abstract class AbstractPacketTraffic(override val currentIdentifier: String, procrastinator: ProcrastinatorControl) extends PacketTraffic {

    @volatile private var closed     = false
    protected val rootStore          = new SimplePacketInjectableStore(this, Array.empty)
    private   val injectionContainer = new ParallelInjectionContainer()

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

    @inline
    override def processInjection(packet: Packet, attr: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit = {
        processInjection(SimplePacketBundle(packet, attr, coordinates))
    }

    override def processInjection(bundle: PacketBundle): Unit = {
        val injection = injectionContainer.makeInjection(bundle)
        processInjection0(injection)
    }

    private def processInjection0(injection: PacketInjectionController): Unit = {
        injection.synchronized {
            if (injection.isProcessing) {
                //If a thread is already processing the injection, don't do it with this thread.
                return
            }
            injection.markAsProcessing
        }
        //AppLogger.debug(s"Injection is not processing : injection.isProcessing = ${injection.isProcessing}")
        makeProcess(injection)
    }

    private def makeProcess(injection: PacketInjectionController): Unit = {
        //AppLogger.debug(s"Processing injection $injection...")
        ensureOpen()
        rootStore.inject(injection)
        injectionContainer.removeInjection(injection)
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = rootStore.findStore(id)

    override def createStore(id: Int): PacketInjectableStore = rootStore.createStore(id)
}