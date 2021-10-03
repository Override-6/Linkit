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

import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.internal.system.event.{Event, EventHook}

trait PacketEvent extends Event[PacketEventHooks, PacketEventListener] {

    protected type PacketEventHook = EventHook[PacketEventListener, this.type]
    val packet: Packet

    override def getHooks(category: PacketEventHooks): Array[PacketEventHook]
}
