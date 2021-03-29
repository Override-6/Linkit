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

package fr.linkit.core.connection.packet.traffic

import fr.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.system.{AppLogger, ClosedException, JustifiedCloseable, Reason}
import fr.linkit.core.local.concurrency.PacketReaderThread

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

abstract class AbstractPacketTraffic(override val supportIdentifier: String) extends PacketTraffic {

    private  val holders            = mutable.Map.empty[Int, ScopesHolder]
    private  val lostInjections     = mutable.Map.empty[Int, ListBuffer[PacketInjection]]
    override val injectionContainer = new DirectInjectionContainer
    @volatile private var closed    = false

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int,
                                                                 scopeFactory: ScopeFactory[_ <: ChannelScope],
                                                                 factory: PacketInjectableFactory[C]): C = {
        val scope     = scopeFactory(newWriter(id))
        val holderOpt = holders.get(id)
        if (holderOpt.isDefined) {
            val holder  = holderOpt.get
            val attempt = holder.tryRetrieveInjectable(scope)
            if (attempt.isDefined) {
                return attempt.get
            }

            if (holder.canConflict(scope)) {
                throw new ConflictException("This scope can conflict with other scopes that are registered within this injectable identifier")
            }
        }

        completeCreation(scope, factory)
    }

    override def canConflict(identifier: Int, scope: ChannelScope): Boolean = {
        holders
            .get(identifier)
            .exists(_.canConflict(scope))
    }

    private def completeCreation[C <: PacketInjectable](scope: ChannelScope, factory: PacketInjectableFactory[C]): C = {
        val channel = factory.createNew(scope)
        register(scope, channel)
        channel
    }

    private def register(scope: ChannelScope, dedicated: PacketInjectable): Unit = {
        ensureOpen()

        val id = dedicated.identifier
        init(dedicated)

        holders.getOrElseUpdate(id, ScopesHolder(id)).register(scope, dedicated)
    }

    private def init(injectable: PacketInjectable): Unit = {
        val id = injectable.identifier

        //Will inject every lost packets
        lostInjections
            .get(id)
            .foreach(_.foreach(injectable.inject))
        lostInjections.remove(id)
    }

    private def getInjectables(identifier: Int, target: String): Iterable[PacketInjectable] = {
        val opt = holders.get(identifier)

        if (opt.isEmpty)
            return Iterable()

        opt.get.getInjectables(target)
    }

    override def close(reason: Reason): Unit = {
        holders.values
            .foreach(_.close(reason))
        holders.clear()
        closed = true
    }

    override def isClosed: Boolean = closed

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    override def handleInjection(packet: Packet, coordinates: DedicatedPacketCoordinates): Unit = {
        handleInjection(injectionContainer.makeInjection(packet, coordinates))
    }

    override def handleInjection(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        PacketReaderThread.checkNotCurrent()
        ensureOpen()

        val id = coordinates.injectableID

        val sender      = coordinates.senderID
        val injectables = getInjectables(id, sender)
        if (injectables.isEmpty) {
            lostInjections.getOrElseUpdate(id, ListBuffer.empty) += injection
            return
        }
        injectables.foreach(_.inject(injection))
        injectionContainer.removeInjection(injection)
    }

    protected case class ScopesHolder(identifier: Int) extends JustifiedCloseable {

        private val cache  = mutable.Set.empty[(ChannelScope, PacketInjectable)]
        private var closed = false

        override def close(reason: Reason): Unit = {
            for (tuple <- cache if tuple._2.isOpen) try {
                tuple._2.close()
            } catch {
                case NonFatal(e) => AppLogger.printStackTrace(e)
            }
            cache.clear()
            closed = true
        }

        override def isClosed: Boolean = closed

        def canConflict(scope: ChannelScope): Boolean = {
            cache.exists(_._1.canConflictWith(scope))
        }

        def isAuthorised(targetID: String): Boolean = {
            cache.exists(_._1.areAuthorised(targetID))
        }

        def getInjectables(target: String): Seq[PacketInjectable] = {
            cache
                .filter(_._1.areAuthorised(target))
                .map(_._2)
                .toSeq
        }

        def tryRetrieveInjectable[I <: PacketInjectable : ClassTag](scope: ChannelScope): Option[I] = {
            val injectableClass = classTag[I].runtimeClass
            cache.find(tuple => tuple._1 == scope && tuple._2.getClass == injectableClass)
                .map(_._2)
                .asInstanceOf[Option[I]]
        }

        def register(scope: ChannelScope, injectable: PacketInjectable): Unit = {
            cache += ((scope, injectable))
        }
    }

}