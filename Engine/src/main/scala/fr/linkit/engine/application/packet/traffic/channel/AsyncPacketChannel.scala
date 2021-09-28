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

package fr.linkit.engine.application.packet.traffic.channel

import fr.linkit.api.application.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.application.packet.traffic._
import fr.linkit.api.application.packet.{ChannelPacketBundle, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.internal.concurrency.{WorkerPools, workerExecution}
import fr.linkit.engine.application.packet.{SimplePacketAttributes, SimplePacketBundle}
import fr.linkit.engine.internal.utils.ConsumerContainer

class AsyncPacketChannel protected(store: PacketInjectableStore, scope: ChannelScope)
    extends AbstractPacketChannel(store, scope) with PacketSender with PacketAsyncReceiver[PacketBundle] {

    private val packetReceivedContainer: ConsumerContainer[PacketBundle] = ConsumerContainer()

    @workerExecution
    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        val pool = WorkerPools.currentPool.get
        pool.runLater {
            packetReceivedContainer.applyAll(bundle)
        }
    }

    override def send(packet: Packet, attributes: PacketAttributes): Unit = {
        scope.sendToAll(packet, attributes)
    }

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit = {
        scope.sendTo(packet, attributes, targets)
    }

    override def send(packet: Packet): Unit = send(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, targets: Array[String]): Unit = sendTo(packet, SimplePacketAttributes.empty, targets)

    override def addOnPacketReceived(callback: PacketBundle => Unit): Unit = {
        packetReceivedContainer += callback
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(store, scope)
    }

}