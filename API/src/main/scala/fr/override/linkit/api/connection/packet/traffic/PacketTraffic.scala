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

package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.JustifiedCloseable


trait PacketTraffic extends JustifiedCloseable with PacketInjectableContainer  {

    val relayID: String
    val ownerID: String

    @workerExecution
    def handleInjection(injection: PacketInjection): Unit

    def canConflict(id: Int, scope: ChannelScope): Boolean

    def newWriter(id: Int, transform: Packet => Packet = p => p): PacketWriter

    def translator: PacketTranslator

}

object PacketTraffic {
    val SystemChannelID = 1
    val RemoteConsoles = 2
}