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

package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.traffic.PacketInjection
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}

import java.util.concurrent.ConcurrentHashMap

object PacketInjections {
    private[traffic] val currentInjections = new ConcurrentHashMap[(Int, String), DirectInjection]

    def createInjection(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): PacketInjection = this.synchronized {
        //println(s"CREATING INJECTION FOR PACKET $packet WITH COORDINATES $coordinates, $number")
        val id = coordinates.injectableID
        val sender = coordinates.senderID

        var injection = currentInjections.get((id, sender))
        if (injection == null) {
            injection = new DirectInjection(coordinates)
            currentInjections.put((id, sender), injection)
        }

        injection.addPacket(number, packet)
        injection
    }

    def unhandled(coordinates: DedicatedPacketCoordinates, packets: Packet*): PacketInjection = {
        //println(s"NEW UNHANDLED FOR COORDINATES $coordinates / PACKETS ${packets}")
        val injection = new DirectInjection(coordinates)
        var i = 0
        packets.foreach(packet => {
            i += 1
            injection.addPacket(i, packet)
        })
        injection
    }

}

