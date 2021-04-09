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

package fr.linkit.core.local.system

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.PacketInjectableFactory
import fr.linkit.api.local.system.Reason
import fr.linkit.core.connection.packet.traffic.channel.SyncPacketChannel

class SystemPacketChannel(scope: ChannelScope)
        extends SyncPacketChannel(null, scope, true) {

    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        send(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel extends PacketInjectableFactory[SystemPacketChannel] {

    override def createNew(parent: PacketChannel, scope: ChannelScope): SystemPacketChannel = {
        if (parent != null)
            throw new IllegalArgumentException("System Packet Channel can't have any parent.")
        new SystemPacketChannel(scope)
    }
}
