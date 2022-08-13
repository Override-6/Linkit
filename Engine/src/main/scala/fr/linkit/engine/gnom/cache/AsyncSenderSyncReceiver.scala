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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableStore, PacketSender, PacketSyncReceiver}
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.traffic.channel.SyncAsyncPacketChannel

import scala.reflect.ClassTag

class AsyncSenderSyncReceiver(store: PacketInjectableStore, scope: ChannelScope) extends SyncAsyncPacketChannel(store, scope) with PacketSender with PacketSyncReceiver {

    override def nextPacket[P <: Packet : ClassTag]: P = nextSync

    override def haveMorePackets: Boolean = false

    override def send(packet: Packet, attributes: PacketAttributes): Unit = sendAsync(packet, attributes)

    override def sendTo(packet: Packet, attributes: PacketAttributes, targets: Array[String]): Unit = sendAsync(packet, attributes, targets)

    override def send(packet: Packet): Unit = send(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, targets: Array[String]): Unit = sendTo(packet, SimplePacketAttributes.empty, targets)
}
