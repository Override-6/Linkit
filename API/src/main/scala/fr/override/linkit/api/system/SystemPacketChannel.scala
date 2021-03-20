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

package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.traffic.channel.SyncPacketChannel
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectableFactory}

class SystemPacketChannel(scope: ChannelScope)
        extends SyncPacketChannel(scope, true) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        send(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel extends PacketInjectableFactory[SystemPacketChannel] {
    override def createNew(scope: ChannelScope): SystemPacketChannel = {
        new SystemPacketChannel(scope)
    }
}
