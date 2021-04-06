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

import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketChannel, PacketSender, PacketSyncReceiver}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

class AsyncSenderSyncReceiver(@Nullable parent: PacketChannel, scope: ChannelScope) extends SyncAsyncPacketChannel(parent, scope, true) with PacketSender with PacketSyncReceiver {

    override def nextPacket[P <: Packet : ClassTag]: P = nextSync

    override def haveMorePackets: Boolean = false

    override def send(packet: Packet, attributes: PacketAttributes): Unit = sendAsync(packet, attributes)

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: String*): Unit = sendAsync(packet, attributes, targets: _*)

    override def send(packet: Packet): Unit = send(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, targets: String*): Unit = sendTo(packet, SimplePacketAttributes.empty, targets: _*)
}
