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
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.packet.{ChannelPacketBundle, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.internal.util.ConsumerContainer

class AsyncPacketChannel protected(scope: ChannelScope)
    extends AbstractPacketChannel(scope) with PacketSender with PacketAsyncReceiver[PacketBundle] {

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

    override def createNew(scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(scope)
    }

}