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

package fr.linkit.engine.internal.system.event.packet

import fr.linkit.api.internal.system.event.EventListener
import fr.linkit.engine.internal.system.event.packet.PacketEvents._

abstract class PacketEventListener extends EventListener {

    def onPacketWritten(event: PacketWrittenEvent): Unit = ()

    def onPacketSent(event: PacketSentEvent): Unit = ()

    def onDedicatedPacketSent(event: DedicatedPacketSentEvent): Unit = ()

    def onBroadcastPacketSent(event: BroadcastPacketSentEvent): Unit = ()

    def onPacketReceived(event: PacketReceivedEvent): Unit = ()

    def onPacketInjected(event: PacketInjectedEvent): Unit = ()

}
