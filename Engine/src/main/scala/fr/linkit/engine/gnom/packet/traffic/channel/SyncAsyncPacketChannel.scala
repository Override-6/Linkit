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

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.gnom.packet.{ChannelPacketBundle, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.traffic.channel.SyncAsyncPacketChannel.Attribute
import fr.linkit.engine.internal.util.ConsumerContainer
import fr.linkit.engine.internal.util.ScalaUtils.ensurePacketType

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.reflect.ClassTag

class SyncAsyncPacketChannel(store: PacketInjectableStore,
                             scope: ChannelScope)
        extends AbstractPacketChannel(store, scope) {

    private val sync: BlockingQueue[Packet] = WorkerPools.ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())

    private val asyncListeners = new ConsumerContainer[PacketBundle]

    @workerExecution
    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        val attr   = bundle.attributes
        val packet = bundle.packet
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
        val take = sync.take()
        ensurePacketType[P](take)
    }

    def sendAsync(packet: Packet): Unit = {
        sendSync(packet, SimplePacketAttributes.empty)
    }

    def sendAsync(packet: Packet, attributes: PacketAttributes): Unit = {
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

    def sendSync(packet: Packet, attributes: PacketAttributes): Unit = {
        attributes.putAttribute(Attribute, false)
        drainAllAttributes(attributes)
        scope.sendToAll(packet, attributes)
    }

    def sendSync(packet: Packet): Unit = {
        sendSync(packet, SimplePacketAttributes.empty)
    }

}

object SyncAsyncPacketChannel extends PacketInjectableFactory[SyncAsyncPacketChannel] {

    val Attribute: Int = 5

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): SyncAsyncPacketChannel = {
        new SyncAsyncPacketChannel(store, scope)
    }

}