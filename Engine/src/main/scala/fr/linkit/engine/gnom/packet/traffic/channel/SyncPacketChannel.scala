/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.packet.traffic.channel

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableFactory, PacketInjectableStore, PacketSender, PacketSyncReceiver}
import fr.linkit.api.gnom.packet.{ChannelPacketBundle, Packet, PacketAttributes}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.system.Reason
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.utils.ScalaUtils.ensurePacketType

import scala.reflect.ClassTag

//TODO doc
class SyncPacketChannel protected(store: PacketInjectableStore, scope: ChannelScope,
                                  providable: Boolean) extends AbstractPacketChannel(store, scope)
    with PacketSender with PacketSyncReceiver {

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            WorkerPools
                .ifCurrentWorkerOrElse(_.newBusyQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        queue.add(bundle.packet)
    }

    override def send(packet: Packet): Unit = scope.sendToAll(packet)

    override def sendTo(packet: Packet, targets: Array[String]): Unit = {
        scope.sendTo(packet, targets)
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

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit = {
        scope.sendTo(packet, attributes, targets)
    }
}

object SyncPacketChannel extends PacketInjectableFactory[SyncPacketChannel] {

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): SyncPacketChannel = {
        new SyncPacketChannel(store, scope, false)
    }

    def providable: PacketInjectableFactory[SyncPacketChannel] = new SyncPacketChannel(_, _, true)

}