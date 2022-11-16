/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.channel

import fr.linkit.api.gnom.network.tag.Server
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.persistence.obj.TrafficObjectReference
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.api.internal.system.Reason
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence
import fr.linkit.engine.gnom.packet.traffic.DefaultPacketChannelBundle

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractPacketChannel(scope: ChannelScope) extends AbstractAttributesPresence with PacketChannel with PacketInjectable {

    //protected but not recommended to use for implementations.
    //it could occurs of unexpected behaviors by the user.
    protected      val writer       : PacketWriter                     = scope.writer
    override       val ownerTag                                        = Server
    override       val trafficPath  : Array[Int]                       = writer.path
    override       val traffic      : PacketTraffic                    = writer.traffic
    private        val storedBundles: mutable.Set[ChannelPacketBundle] = mutable.HashSet.empty[ChannelPacketBundle]
    override final val reference    : TrafficObjectReference           = makeReference
    override lazy  val presence     : NetworkObjectPresence            = traffic.getTrafficObjectLinker.getPresence(reference)


    @volatile private var closed = true

    protected def makeReference: TrafficObjectReference = new TrafficObjectReference(trafficPath)

    override def close(reason: Reason): Unit = closed = true

    override def isClosed: Boolean = closed

    final override def inject(bundle: PacketBundle): Unit = {
        val coordinates = bundle.coords
        scope.assertAuthorised(coordinates.senderTag)
        handleBundle(DefaultPacketChannelBundle(this, bundle))
    }


    override def storeBundle(bundle: ChannelPacketBundle): Unit = {
        AppLoggers.GNOM.trace(s"in packet channel $reference: storing bundle $bundle.")
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
            AppLoggers.GNOM.trace(s"in packet channel $reference: reinjecting stored bundles (size: ${storedBundles.size})")
            clone = Array.from(storedBundles)
            storedBundles.clear()
        }

        val builder = ListBuffer.newBuilder
        builder.sizeHint(clone.length)
        val injected = builder.result()

        clone.foreach(stored => {
            AppLoggers.GNOM.trace(s"in packet channel $reference: reinjecting stored bundle $stored")
            if (injected.contains(stored))
                throw new Error("Double instance packet in storage.")

            inject(stored)
        })
    }

    def handleBundle(injection: ChannelPacketBundle): Unit

}
