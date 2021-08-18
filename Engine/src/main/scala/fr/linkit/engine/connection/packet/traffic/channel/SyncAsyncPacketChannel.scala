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

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.connection.packet.{ChannelPacketBundle, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.local.concurrency.{WorkerPools, workerExecution}
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel.Attribute
import fr.linkit.engine.connection.packet.{SimplePacketAttributes, SimplePacketBundle}
import fr.linkit.engine.local.utils.ConsumerContainer
import fr.linkit.engine.local.utils.ScalaUtils.ensurePacketType
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

class SyncAsyncPacketChannel(store: PacketInjectableStore,
                             scope: ChannelScope,
                             busy: Boolean)
    extends AbstractPacketChannel(store, scope) {

    private val sync: BlockingQueue[Packet] = {
        if (!busy) {
            new LinkedBlockingQueue[Packet]()
        } else {
            WorkerPools
                .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val asyncListeners = new ConsumerContainer[PacketBundle]

    @workerExecution
    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        val attr        = bundle.attributes
        val packet      = bundle.packet
        val coordinates = bundle.coords
        attr.getAttribute[Boolean](Attribute) match {
            case Some(isAsync) =>
                if (isAsync)
                    asyncListeners.applyAll(bundle)
                else
                    sync.add(packet)
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

    def sendAsync(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit = {
        attributes.putAttribute(Attribute, true)
        drainAllAttributes(attributes)
        scope.sendTo(packet, attributes, targets)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit = {
        attributes.putAttribute(Attribute, false)
        drainAllAttributes(attributes)
        scope.sendTo(packet, targets)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        attributes.putAttribute(Attribute, false)
        drainAllAttributes(attributes)
        scope.sendToAll(packet, attributes)
    }

}

object SyncAsyncPacketChannel extends PacketInjectableFactory[SyncAsyncPacketChannel] {

    val Attribute: Int = 5

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): SyncAsyncPacketChannel = {
        new SyncAsyncPacketChannel(store, scope, false)
    }

    def busy: PacketInjectableFactory[SyncAsyncPacketChannel] = new SyncAsyncPacketChannel(_, _, true)

}
