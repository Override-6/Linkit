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

package fr.linkit.core.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableFactory, PacketInjection}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.utils.ScalaUtils.ensureType
import fr.linkit.core.local.utils.{ConsumerContainer, ScalaUtils}

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.reflect.ClassTag

class SyncAsyncPacketChannel(scope: ChannelScope,
                             busy: Boolean)
    extends AbstractPacketChannel(scope) {

    private val sync: BlockingQueue[Packet] = {
        if (!busy)
            new LinkedBlockingQueue[Packet]()
        else {
            BusyWorkerPool
                .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val asyncListeners = new ConsumerContainer[(Packet, DedicatedPacketCoordinates)]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        injection.attachPin {
            case WrappedPacket(tag, subPacket) =>
                tag match {
                    case "s" => sync.add(subPacket)
                    case "a" => asyncListeners.applyAll((subPacket, coordinates))
                }
        }
        //println(s"<$identifier> sync = ${sync}")
    }

    def addAsyncListener(action: (Packet, DedicatedPacketCoordinates) => Unit): Unit =
        asyncListeners += (tuple => action(tuple._1, tuple._2))

    def nextSync[P <: Packet : ClassTag]: P = {
        ensureType[P](sync.take())
    }

    def sendAsync(packet: Packet): Unit = {
        scope.sendToAll(WrappedPacket("a", packet))
    }

    def sendAsync(packet: Packet, targets: String*): Unit = {
        scope.sendTo(WrappedPacket("a", packet), targets: _*)
    }

    def sendSync(packet: Packet, targets: String*): Unit = {
        scope.sendTo(WrappedPacket("s", packet), targets: _*)
    }

}

object SyncAsyncPacketChannel extends PacketInjectableFactory[SyncAsyncPacketChannel] {

    override def createNew(scope: ChannelScope): SyncAsyncPacketChannel = {
        new SyncAsyncPacketChannel(scope, false)
    }

    def busy: PacketInjectableFactory[SyncAsyncPacketChannel] = new SyncAsyncPacketChannel(_, true)

}
