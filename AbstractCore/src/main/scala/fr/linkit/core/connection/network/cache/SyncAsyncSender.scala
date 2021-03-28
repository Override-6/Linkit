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

package fr.linkit.core.connection.network.cache

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketSender, PacketSyncReceiver}
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel

import scala.reflect.ClassTag

class SyncAsyncSender(scope: ChannelScope) extends SyncAsyncPacketChannel(scope, true) with PacketSender with PacketSyncReceiver {

    override def send(packet: Packet): Unit = sendAsync(packet)

    override def sendTo(packet: Packet, targets: String*): Unit = sendAsync(packet, targets: _*)

    override def nextPacket[P <: Packet : ClassTag]: P = nextSync

    override def haveMorePackets: Boolean = false
}
