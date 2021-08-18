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

import fr.linkit.api.connection
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.{WorkerPools, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine
import fr.linkit.engine.connection.packet.{SimplePacketBundle, SimplePacketAttributes}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.utils.ConsumerContainer
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class AsyncPacketChannel protected(@Nullable parent: PacketChannel, scope: ChannelScope)
        extends AbstractPacketChannel(parent, scope) with PacketSender with PacketAsyncReceiver[SimplePacketBundle] {

    private val packetReceivedContainer: ConsumerContainer[SimplePacketBundle] = ConsumerContainer()

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val pool = WorkerPools.currentPool.get
        pool.runLater {
            injection.attachPin((packet, attr) => {
                try {
                    val coords = injection.coordinates
                    packetReceivedContainer.applyAll(engine.connection.packet.SimplePacketBundle(this, packet, attr, coords))
                } catch {
                    case NonFatal(e) =>
                        AppLogger.printStackTrace(e)
                }
            })
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

    override def addOnPacketReceived(callback: SimplePacketBundle => Unit): Unit = {
        packetReceivedContainer += callback
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(parent: PacketChannel, scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(parent, scope)
    }

}