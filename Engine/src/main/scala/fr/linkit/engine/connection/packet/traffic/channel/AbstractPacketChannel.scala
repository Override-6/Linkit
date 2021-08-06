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

package fr.linkit.engine.connection.packet.traffic.channel

import fr.linkit.api.connection.packet._
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.{AppLogger, ForbiddenIdentifierException, Reason}
import fr.linkit.engine.connection.packet.AbstractAttributesPresence
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractPacketChannel(@Nullable parent: PacketChannel, scope: ChannelScope) extends AbstractAttributesPresence with PacketChannel with PacketInjectable {

    //protected but not recommended to use for implementations.
    //it could occurs of unexpected behaviors by the user.
    protected val writer    : PacketWriter  = scope.writer
    override  val ownerID   : String        = writer.serverIdentifier
    override  val identifier: Int           = writer.injectableID
    override  val traffic   : PacketTraffic = writer.traffic
    private   val subChannels               = mutable.Set.empty[SubInjectableContainer]
    private   val storedBundles             = mutable.HashSet.empty[Bundle]

    @volatile private var closed = true

    override def close(reason: Reason): Unit = closed = true

    override def isClosed: Boolean = closed

    @workerExecution
    final override def inject(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        scope.assertAuthorised(Array(coordinates.senderID))
        if (subInject(injection)) {
            handleInjection(injection)
        }
    }

    override def canInjectFrom(identifier: String): Boolean = scope.areAuthorised(Array(identifier))

    override def subInjectable[C <: PacketInjectable](scopes: Array[String],
                                                      factory: PacketInjectableFactory[C],
                                                      transparent: Boolean): C = {
        if (scopes.exists(id => !scope.areAuthorised(Array(id))))
            throw new ForbiddenIdentifierException("This sub injector requests to listen to an identifier that the parent does not support.")

        val subScope = ChannelScopes.retains(scopes: _*).apply(writer)
        register(subScope, factory, transparent)
    }

    override def subInjectable[C <: PacketInjectable](factory: PacketInjectableFactory[C], transparent: Boolean): C = {
        register(scope, factory, transparent)
    }

    override def getParent: Option[PacketChannel] = Option(parent)

    override def storeBundle(bundle: Bundle): Unit = {
        storedBundles.synchronized {
            AppLogger.vDebug(s"$currentTasksId <> STORING BUNDLE $bundle INTO $storedBundles")
        }
        if (bundle.getChannel.identifier != identifier) {
            throw new IllegalArgumentException("Stored packet coordinates must target the same injectable identifier as this channel.")
        }

        storedBundles.synchronized {
            storedBundles += bundle
        }
    }

    override def injectStoredBundles(): Unit = {
        var clone: Array[Bundle] = null
        storedBundles.synchronized {
            AppLogger.vDebug(s"$currentTasksId <> REINJECTING STORED PACKETS $storedBundles")
            clone = Array.from(storedBundles)
            storedBundles.clear()
        }

        val builder = ListBuffer.newBuilder
        builder.sizeHint(clone.length)
        val injected = builder.result()

        clone.foreach(stored => {
            AppLogger.vDebug(s"$currentTasksId <> Reinjecting stored = $stored")
            if (injected.contains(stored))
                throw new Error("Double instance packet in storage.")

            val injection = traffic.injectionContainer.makeInjection(stored)
            inject(injection)
        })
    }

    @workerExecution
    def handleInjection(injection: PacketInjection): Unit

    private def register[C <: PacketInjectable](scope: ChannelScope,
                                                factory: PacketInjectableFactory[C],
                                                transparent: Boolean): C = {
        val channel = factory.createNew(this, scope)
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

            authoriseInject = authoriseInject && container.transparent
        }
        authoriseInject
    }

    protected case class SubInjectableContainer(subInjectable: PacketInjectable, transparent: Boolean)

}
