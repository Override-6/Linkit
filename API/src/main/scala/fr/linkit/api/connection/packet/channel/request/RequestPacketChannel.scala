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

package fr.linkit.api.connection.packet.channel.request

import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.PacketInjectable

trait RequestPacketChannel extends PacketChannel with PacketInjectable {

    def addRequestListener(callback: RequestPacketBundle => Unit): Unit

    def makeRequest(scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): Submitter[ResponseHolder]

    def makeRequest(scope: ChannelScope): Submitter[ResponseHolder]
}
