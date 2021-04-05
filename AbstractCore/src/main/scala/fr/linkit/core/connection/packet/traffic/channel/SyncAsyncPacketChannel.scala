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
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.EmptyPacket
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel.nextChannelNumber
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.utils.ConsumerContainer
import fr.linkit.core.local.utils.ScalaUtils.ensureType

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

    private val channelNumber = nextChannelNumber

    private val asyncListeners = new ConsumerContainer[(Packet, PacketAttributes, DedicatedPacketCoordinates)]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        injection.attachPin { (packet, attr) =>
            attr.getPresence[String](this) match {
                case Some(tag) =>
                    tag match {
                        case "s" => sync.add(packet)
                        case "a" => asyncListeners.applyAll((packet, attr, coordinates))
                    }
            }
        }
        //println(s"<$identifier> sync = ${sync}")
    }

    def addAsyncListener(action: (Packet, PacketAttributes, DedicatedPacketCoordinates) => Unit): Unit =
        asyncListeners += (tuple => action(tuple._1, tuple._2, tuple._3))

    def nextSync[P <: Packet : ClassTag]: P = {
        ensureType[P](sync.take())
    }

    def sendAsync(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        attributes.putPresence(this, true)
        scope.sendToAll(packet, attributes)
    }

    sendAsync(EmptyPacket)

    def sendAsync(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = {
        attributes.putPresence(this, true)
        scope.sendTo(packet, attributes, targets: _*)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = {
        attributes.putPresence(this, true)
        scope.sendTo(packet, targets: _*)
    }

    def sendSync(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        attributes.putPresence(this, false)
        scope.sendToAll(packet, attributes)
    }

}

object SyncAsyncPacketChannel extends PacketInjectableFactory[SyncAsyncPacketChannel] {

    @volatile private var channelNumber: Int = 0

    private def nextChannelNumber: Int = {
        channelNumber += 1
        channelNumber
    }

    override def createNew(scope: ChannelScope): SyncAsyncPacketChannel = {
        new SyncAsyncPacketChannel(scope, false)
    }

    def busy: PacketInjectableFactory[SyncAsyncPacketChannel] = new SyncAsyncPacketChannel(_, true)

}
