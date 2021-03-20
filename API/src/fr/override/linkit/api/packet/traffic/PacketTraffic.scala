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

package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.system.JustifiedCloseable

import scala.reflect.ClassTag


trait PacketTraffic extends JustifiedCloseable {

    val relayID: String
    val ownerID: String

    def createInjectable[C <: PacketInjectable : ClassTag](id: Int,
                                                           scopeFactory: ScopeFactory[_ <: ChannelScope],
                                                           factory: PacketInjectableFactory[C]): C

    @relayWorkerExecution
    def handleInjection(injection: PacketInjection): Unit

    def canConflict(id: Int, scope: ChannelScope): Boolean

    def newWriter(id: Int, transform: Packet => Packet = p => p): PacketWriter

}

object PacketTraffic {
    val SystemChannelID = 1
    val RemoteConsoles = 2
}