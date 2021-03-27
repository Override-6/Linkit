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

package fr.linkit.core.connection.packet.traffic

import fr.linkit.api.connection.packet.serialization.PacketTranslator
import fr.linkit.api.connection.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet}

class SocketPacketWriter(socket: DynamicSocket,
                         translator: PacketTranslator,
                         info: WriterInfo) extends PacketWriter {

    override val traffic          : PacketTraffic = info.traffic
    override val serverIdentifier : String        = traffic.serverIdentifier
    override val supportIdentifier: String        = traffic.supportIdentifier
    override val identifier       : Int           = info.identifier

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        val transformedPacket = info.transform(packet)

        val coords = if (targetIDs.length == 1) {
            val target    = targetIDs.head
            val dedicated = DedicatedPacketCoordinates(identifier, targetIDs(0), supportIdentifier)
            if (target == supportIdentifier) {
                traffic.handleInjection(PacketInjections.unhandled(dedicated, packet))
                return
            }
            dedicated
        } else {
            if (targetIDs.contains(supportIdentifier))
                traffic.handleInjection(PacketInjections.unhandled(DedicatedPacketCoordinates(identifier, serverIdentifier, supportIdentifier), packet))

            BroadcastPacketCoordinates(identifier, supportIdentifier, false, targetIDs.filter(_ != supportIdentifier): _*)
        }

        socket.write(translator.translate(transformedPacket, coords).writableBytes)
    }

    override def writeBroadcastPacket(packet: Packet, discardedIDs: String*): Unit = {
        val transformedPacket = info.transform(packet)
        val coords            = BroadcastPacketCoordinates(identifier, supportIdentifier, true, discardedIDs: _*)

        socket.write(translator.translate(transformedPacket, coords).writableBytes)
    }

}
