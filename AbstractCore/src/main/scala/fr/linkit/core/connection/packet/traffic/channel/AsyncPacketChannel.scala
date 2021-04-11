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

import fr.linkit.api.connection
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core
import fr.linkit.core.connection.packet.{PacketBundle, SimplePacketAttributes}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.utils.ConsumerContainer
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class AsyncPacketChannel protected(@Nullable parent: PacketChannel, scope: ChannelScope)
        extends AbstractPacketChannel(parent, scope) with PacketSender with PacketAsyncReceiver[PacketBundle] {

    private val packetReceivedContainer: ConsumerContainer[PacketBundle] = ConsumerContainer()

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val pool = BusyWorkerPool.currentPool.get
        pool.runLater {
            injection.attachPin((packet, attr) => {
                try {
                    val coords = injection.coordinates
                    packetReceivedContainer.applyAll(core.connection.packet.PacketBundle(this, packet, attr, coords))
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

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = {
        scope.sendTo(packet, attributes, targets: _*)
    }

    override def send(packet: Packet): Unit = send(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, targets: String*): Unit = sendTo(packet, SimplePacketAttributes.empty, targets: _*)

    override def addOnPacketReceived(callback: PacketBundle => Unit): Unit = {
        packetReceivedContainer += callback
    }
}

object AsyncPacketChannel extends PacketInjectableFactory[AsyncPacketChannel] {

    override def createNew(parent: PacketChannel, scope: ChannelScope): AsyncPacketChannel = {
        new AsyncPacketChannel(parent, scope)
    }

}