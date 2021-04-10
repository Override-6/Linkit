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
import fr.linkit.api.connection.packet._
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.serialization.SimpleTransferInfo

class SocketPacketWriter(socket: DynamicSocket,
                         translator: PacketTranslator,
                         writerInfo: WriterInfo) extends PacketWriter {

    override val traffic          : PacketTraffic = writerInfo.traffic
    override val serverIdentifier : String        = traffic.serverIdentifier
    override val supportIdentifier: String        = traffic.supportIdentifier
    override val identifier       : Int           = writerInfo.identifier

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        writePacket(packet, SimplePacketAttributes.empty, targetIDs: _*)
    }

    override def writePacket(packet: Packet, attributes: PacketAttributes, targetIDs: String*): Unit = {
        val coords       = if (targetIDs.length == 1) {
            val target    = targetIDs.head
            val dedicated = DedicatedPacketCoordinates(identifier, targetIDs(0), supportIdentifier)
            if (target == supportIdentifier) {
                traffic.processInjection(packet, attributes,  dedicated)
                return
            }
            dedicated
        } else {
            if (targetIDs.contains(supportIdentifier)) {
                val coords = DedicatedPacketCoordinates(identifier, serverIdentifier, supportIdentifier)
                traffic.processInjection(packet, attributes,  coords)
            }

            BroadcastPacketCoordinates(identifier, supportIdentifier, false, targetIDs.filter(_ != supportIdentifier): _*)
        }
        val transferInfo = SimpleTransferInfo(coords, attributes, packet)

        val writableBytes = translator.translate(transferInfo).writableBytes
        socket.write(writableBytes)
    }

    override def writeBroadcastPacket(packet: Packet, attributes: PacketAttributes, discardedIDs: String*): Unit = {
        val coords            = BroadcastPacketCoordinates(identifier, supportIdentifier, true, discardedIDs: _*)
        val transferInfo      = SimpleTransferInfo(coords, attributes, packet)

        val writableBytes = translator.translate(transferInfo).writableBytes
        if (!discardedIDs.contains(supportIdentifier))
            traffic.processInjection(packet, attributes, DedicatedPacketCoordinates(coords.injectableID, supportIdentifier, supportIdentifier))
        socket.write(writableBytes)
    }

    override def writeBroadcastPacket(packet: Packet, discardedIDs: String*): Unit = {
        writeBroadcastPacket(packet, SimplePacketAttributes.empty, discardedIDs: _*)
    }

}
