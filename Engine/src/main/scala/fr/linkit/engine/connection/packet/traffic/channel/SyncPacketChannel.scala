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

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.connection.packet.traffic.{PacketInjectableFactory, PacketSender, PacketSyncReceiver}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.Reason
import fr.linkit.engine.local.concurrency.PacketReaderThread
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.utils.ScalaUtils.ensurePacketType
import org.jetbrains.annotations.Nullable

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.reflect.ClassTag

//TODO doc
class SyncPacketChannel protected(@Nullable parent: PacketChannel, scope: ChannelScope,
                                  providable: Boolean) extends AbstractPacketChannel(parent, scope)
        with PacketSender with PacketSyncReceiver {

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            BusyWorkerPool
                    .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        injection.attachPinPacket(queue.add)
    }

    override def send(packet: Packet): Unit = scope.sendToAll(packet)

    override def sendTo(packet: Packet, targets: String*): Unit = {
        scope.sendTo(packet, targets: _*)
    }

    override def close(reason: Reason): Unit = {
        super.close(reason)
        queue.clear()
    }

    override def nextPacket[P <: Packet : ClassTag]: P = {
        if (queue.isEmpty)
            PacketReaderThread.checkNotCurrent()

        val packet = queue.take()
        ensurePacketType[P](packet)
    }

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] would not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty

    override def send(packet: Packet, attributes: PacketAttributes): Unit = scope.sendToAll(packet, attributes)

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = scope.sendTo(packet, attributes, targets: _*)
}

object SyncPacketChannel extends PacketInjectableFactory[SyncPacketChannel] {

    override def createNew(@Nullable parent: PacketChannel, scope: ChannelScope): SyncPacketChannel = {
        new SyncPacketChannel(parent, scope, false)
    }

    def providable: PacketInjectableFactory[SyncPacketChannel] = new SyncPacketChannel(_, _, true)

}