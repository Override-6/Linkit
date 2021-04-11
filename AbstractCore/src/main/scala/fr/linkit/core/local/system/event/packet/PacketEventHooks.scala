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

package fr.linkit.core.local.system.event.packet

import fr.linkit.api.local.system.event.EventHookCategory
import fr.linkit.core.local.system.event.SimpleEventHook
import fr.linkit.core.local.system.event.packet.PacketEvents._

//noinspection TypeAnnotation
class PacketEventHooks extends EventHookCategory {

    type L = PacketEventListener
    val packetWritten = SimpleEventHook[L, PacketWrittenEvent](_.onPacketWritten(_))

    val packetSent: Unit = SimpleEventHook[L, PacketSentEvent](_.onPacketSent(_))

    val dedicatedPacketSent = SimpleEventHook[L, DedicatedPacketSentEvent](_.onDedicatedPacketSent(_))

    val broadcastPacketSent = SimpleEventHook[L, BroadcastPacketSentEvent](_.onBroadcastPacketSent(_))

    val packetReceived = SimpleEventHook[L, PacketReceivedEvent](_.onPacketReceived(_))

    val packetInjected = SimpleEventHook[L, PacketInjectedEvent](_.onPacketInjected(_))
}
