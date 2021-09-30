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

package fr.linkit.engine.gnom.packet.traffic.channel

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.{AppLogger, Reason}
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence
import fr.linkit.engine.gnom.packet.traffic.DefaultChannelPacketBundle

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractPacketChannel(override val store: PacketInjectableStore,
                                     scope: ChannelScope) extends AbstractAttributesPresence with PacketChannel with PacketInjectable {

    //protected but not recommended to use for implementations.
    //it could occurs of unexpected behaviors by the user.
    protected val writer : PacketWriter  = scope.writer
    override  val ownerID    : String        = writer.serverIdentifier
    override  val trafficPath: Array[Int]    = writer.path
    override  val traffic    : PacketTraffic = writer.traffic
    private   val storedBundles          = mutable.HashSet.empty[ChannelPacketBundle]

    @volatile private var closed = true

    override def close(reason: Reason): Unit = closed = true

    override def isClosed: Boolean = closed

    @workerExecution
    final override def inject(bundle: PacketBundle): Unit = {
        val coordinates = bundle.coords
        scope.assertAuthorised(Array(coordinates.senderID))
        handleBundle(DefaultChannelPacketBundle(this, bundle))
    }

    override def canInjectFrom(identifier: String): Boolean = scope.areAuthorised(Array(identifier))

    override def storeBundle(bundle: ChannelPacketBundle): Unit = {
        AppLogger.vDebug(s"$currentTasksId <> STORING BUNDLE $bundle INTO $storedBundles")
        if (bundle.getChannel ne this) {
            throw new IllegalArgumentException("The stored bundle's channel is not this.")
        }

        storedBundles.synchronized {
            storedBundles += bundle
        }
    }

    override def injectStoredBundles(): Unit = {
        var clone: Array[ChannelPacketBundle] = null
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

            inject(stored)
        })
    }

    @workerExecution
    def handleBundle(injection: ChannelPacketBundle): Unit

    protected case class SubInjectableContainer(subInjectable: PacketInjectable, transparent: Boolean)

}
