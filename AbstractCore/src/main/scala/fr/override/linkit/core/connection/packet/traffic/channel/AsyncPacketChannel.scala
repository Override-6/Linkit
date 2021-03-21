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

package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.api.connection.packet.traffic._
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.utils.ConsumerContainer

import scala.util.control.NonFatal

class AsyncPacketChannel protected(scope: ChannelScope)
        extends AbstractPacketChannel(scope) with PacketSender with PacketAsyncReceiver {

    private val packetReceivedContainer: ConsumerContainer[(Packet, DedicatedPacketCoordinates)] = ConsumerContainer()

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val pool = BusyWorkerPool.currentPool().get
        pool.runLater {
            try {
                val packets = injection.getPackets
                val coords = injection.coordinates
                packets.foreach(packet => packetReceivedContainer.applyAll((packet, coords)))
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
            }
        }
    }

    override def send(packet: Packet): Unit = {
        scope.sendToAll(packet)
    }

    override def sendTo(packet: Packet, targets: String*): Unit = {
        scope.sendTo(packet, targets: _*)
    }

    override def addOnPacketReceived(callback: (Packet, DedicatedPacketCoordinates) => Unit): Unit = {
        packetReceivedContainer += (tuple => callback(tuple._1, tuple._2))
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(scope)
    }

}