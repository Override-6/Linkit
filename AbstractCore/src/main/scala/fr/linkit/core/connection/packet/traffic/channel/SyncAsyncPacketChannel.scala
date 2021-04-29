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

package fr.linkit.core.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.PacketInjectableFactory
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel.Attribute
import fr.linkit.core.connection.packet.{PacketBundle, SimplePacketAttributes}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.utils.ConsumerContainer
import fr.linkit.core.local.utils.ScalaUtils.ensurePacketType
import org.jetbrains.annotations.Nullable

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.reflect.ClassTag

class SyncAsyncPacketChannel(@Nullable parent: PacketChannel,
                             scope: ChannelScope,
                             busy: Boolean)
        extends AbstractPacketChannel(parent, scope) {

    private val sync: BlockingQueue[Packet] = {
        if (!busy) {
            new LinkedBlockingQueue[Packet]()
        } else {
            BusyWorkerPool
                    .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val asyncListeners = new ConsumerContainer[PacketBundle]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        injection.attachPin { (packet, attr) =>
            attr.getAttribute[Boolean](Attribute) match {
                case Some(isAsync) =>
                    if (isAsync)
                        asyncListeners.applyAll(PacketBundle(this, packet, attr, coordinates))
                    else
                        sync.add(packet)
            }
        }
    }

    def addAsyncListener(action: PacketBundle => Unit): Unit =
        asyncListeners += action

    def nextSync[P <: Packet : ClassTag]: P = {
        ensurePacketType[P](sync.take())
    }

    def sendAsync(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        attributes.putAttribute(Attribute, true)
        drainAllAttributes(attributes)
        scope.sendToAll(packet, attributes)
    }

    def sendAsync(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = {
        attributes.putAttribute(Attribute, true)
        drainAllAttributes(attributes)
        scope.sendTo(packet, attributes, targets: _*)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = {
        attributes.putAttribute(Attribute, false)
        drainAllAttributes(attributes)
        scope.sendTo(packet, targets: _*)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        attributes.putAttribute(Attribute, false)
        drainAllAttributes(attributes)
        scope.sendToAll(packet, attributes)
    }

}

object SyncAsyncPacketChannel extends PacketInjectableFactory[SyncAsyncPacketChannel] {

    val Attribute: Int = 5

    @volatile private var channelNumber: Int = 0

    private def nextChannelNumber: Int = {
        channelNumber += 1
        channelNumber
    }

    override def createNew(@Nullable parent: PacketChannel, scope: ChannelScope): SyncAsyncPacketChannel = {
        new SyncAsyncPacketChannel(parent, scope, false)
    }

    def busy: PacketInjectableFactory[SyncAsyncPacketChannel] = new SyncAsyncPacketChannel(_, _, true)

}
