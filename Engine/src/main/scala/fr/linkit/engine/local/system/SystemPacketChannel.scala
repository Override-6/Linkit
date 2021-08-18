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

package fr.linkit.engine.local.system

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.local.system.Reason
import fr.linkit.engine.connection.packet.traffic.channel.SyncPacketChannel

class SystemPacketChannel(store: PacketInjectableStore, scope: ChannelScope)
    extends SyncPacketChannel(store, scope, true) {

    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        send(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel extends PacketInjectableFactory[SystemPacketChannel] {

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): SystemPacketChannel = {
        new SystemPacketChannel(store, scope)
    }
}
