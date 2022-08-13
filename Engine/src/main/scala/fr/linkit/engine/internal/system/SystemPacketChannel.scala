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

package fr.linkit.engine.internal.system

import fr.linkit.api.gnom.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.internal.system.Reason
import fr.linkit.engine.gnom.packet.traffic.channel.SyncPacketChannel

class SystemPacketChannel(store: PacketInjectableStore, scope: ChannelScope)
    extends SyncPacketChannel(store, scope) {

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
