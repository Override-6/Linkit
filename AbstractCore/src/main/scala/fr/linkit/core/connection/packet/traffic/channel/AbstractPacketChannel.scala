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

package fr.linkit.core.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet, PacketCoordinates}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.{AppLogger, ForbiddenIdentifierException, Reason}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractPacketChannel(scope: ChannelScope) extends PacketChannel with PacketInjectable {

    //protected but not recommended to use for implementations.
    //it could occurs of unexpected behaviors by the user.
    protected val writer    : PacketWriter  = scope.writer
    override  val ownerID   : String        = writer.serverIdentifier
    override  val identifier: Int           = writer.identifier
    override  val traffic   : PacketTraffic = writer.traffic
    private   val subChannels               = mutable.Set.empty[SubInjectableContainer]
    private   val storedPackets             = ListBuffer.empty[(Packet, DedicatedPacketCoordinates)]

    @volatile private var closed = true

    override def close(reason: Reason): Unit = closed = true

    override def isClosed: Boolean = closed

    @workerExecution
    final override def inject(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        scope.assertAuthorised(coordinates.senderID)
        if (subInject(injection)) {
            handleInjection(injection)
        }
    }

    override def canInjectFrom(identifier: String): Boolean = scope.areAuthorised(identifier)

    override def subInjectable[C <: PacketInjectable](scopes: Array[String],
                                                      factory: PacketInjectableFactory[C],
                                                      transparent: Boolean): C = {
        if (scopes.exists(!scope.areAuthorised(_)))
            throw new ForbiddenIdentifierException("This sub injector requests to listen to an identifier that the parent does not support.")

        val subScope = ChannelScope.reserved(scopes: _*).apply(writer)
        register(subScope, factory, transparent)
    }

    override def subInjectable[C <: PacketInjectable](factory: PacketInjectableFactory[C], transparent: Boolean): C = {
        register(scope, factory, transparent)
    }

    def storePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        AppLogger.debug(s"$currentTasksId <> STORING PACKET $packet AND COORDS $coords")
        if (coords.injectableID != identifier) {
            throw new IllegalArgumentException("Stored packet coordinates must target the same injectable identifier as this channel.")
        }

        val supportIdentifier = scope.writer.supportIdentifier
        val directCoords      = coords match {
            case dedicated: DedicatedPacketCoordinates =>
                //if (dedicated.targetID != supportIdentifier) {
                //    throw new IllegalArgumentException("Stored packet coordinates must target current connection.")
                //}

                dedicated
            case broadcast: BroadcastPacketCoordinates =>
                if (broadcast.targetIDs.contains(supportIdentifier) == broadcast.discardTargets) {
                    throw new IllegalArgumentException("Stored packet coordinates must target at least current connection.")
                }

                DedicatedPacketCoordinates(identifier, supportIdentifier, broadcast.senderID)
        }
        storedPackets += ((packet, directCoords))
    }

    def injectStoredPackets(): Unit = {
        AppLogger.debug(s"$currentTasksId <> REINJECTING STORED PACKETS $storedPackets")
        Array.from(storedPackets)
            .foreach(stored => {
                AppLogger.debug(s"$currentTasksId <> stored = $stored")
                val injection = traffic.injectionContainer.makeInjection(stored._1, stored._2)
                inject(injection)
            })
        storedPackets.clear()
    }

    @workerExecution
    def handleInjection(injection: PacketInjection): Unit

    private def register[C <: PacketInjectable](scope: ChannelScope,
                                                factory: PacketInjectableFactory[C],
                                                transparent: Boolean): C = {
        val channel = factory.createNew(scope)
        subChannels += SubInjectableContainer(channel, transparent)
        channel
    }

    /**
     * @return true if the injection can be performed into this channel
     *         the boolean returned depends on the sub injectables.
     *         if one injectable is injected and is not transparent, this method will return false, so
     *         the current injectable could not handle packets for it.
     * */
    private def subInject(injection: PacketInjection): Boolean = {
        val coords          = injection.coordinates
        val target          = coords.targetID
        var authoriseInject = true

        for (container <- subChannels if container.subInjectable.canInjectFrom(target)) {
            //println(s"FOR container = ${container}")
            val injectable = container.subInjectable
            injectable.inject(injection)

            authoriseInject = authoriseInject && !container.transparent
        }
        authoriseInject
    }

    protected case class SubInjectableContainer(subInjectable: PacketInjectable, transparent: Boolean)

}
