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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionController
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.internal.system.{ClosedException, Reason}
import fr.linkit.engine.gnom.packet.SimplePacketBundle
import fr.linkit.engine.gnom.persistence.context.{ImmutablePersistenceContext, PersistenceConfigBuilder}
import fr.linkit.engine.gnom.packet.traffic.injection.ParallelInjectionContainer
import fr.linkit.engine.internal.utils.ClassMap

import java.net.URL
import scala.reflect.ClassTag

abstract class AbstractPacketTraffic(override val currentIdentifier: String,
                                     defaultPersistenceConfigUrl: Option[URL]) extends PacketTraffic {

    val context: ImmutablePersistenceContext = ImmutablePersistenceContext(this, new ClassMap(), new ClassMap())

    override val defaultPersistenceConfig: PersistenceConfig = {
        val builder = defaultPersistenceConfigUrl.fold(new PersistenceConfigBuilder())(PersistenceConfigBuilder.fromScript(_, this))
        builder.build(context)
    }

    @volatile private var closed          = false
    override  val trafficPath: Array[Int] = Array.empty
    protected val rootStore               = new SimplePacketInjectableStore(this, defaultPersistenceConfig, trafficPath)
    private   val injectionContainer      = new ParallelInjectionContainer()

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C = {
        rootStore.getInjectable[C](injectableID, config, factory, scopeFactory)
    }

    override def getPersistenceConfig(path: Array[Int]): PersistenceConfig = rootStore.getPersistenceConfig(path)

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

    override def newWriter(path: Array[Int]): PacketWriter = {
        newWriter(path, defaultPersistenceConfig)
    }

    private def processInjection0(injection: PacketInjectionController): Unit = {
        injection.synchronized {
            if (injection.isProcessing) {
                //If a thread is already processing the injection, don't do it with this thread.
                return
            }
            injection.markAsProcessing()
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

    override def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore = rootStore.createStore(id, persistenceConfig)
}