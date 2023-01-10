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

package fr.linkit.api.gnom.packet.channel.request

import fr.linkit.api.gnom.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.gnom.packet.traffic.PacketInjectable

trait RequestPacketChannel extends PacketChannel with PacketInjectable {

    def addRequestListener(callback: RequestPacketBundle => Unit): Unit

    def makeRequest(scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): Submitter[ResponseHolder]

    def makeRequest(scope: ChannelScope): Submitter[ResponseHolder]
}
