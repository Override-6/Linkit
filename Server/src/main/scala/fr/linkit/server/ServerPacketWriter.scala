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

package fr.linkit.server

import fr.linkit.api.connection.NoSuchConnectionException
import fr.linkit.api.connection.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.core.connection.packet.traffic.{PacketInjections, WriterInfo}
import fr.linkit.server.connection.ServerConnection

class ServerPacketWriter(serverConnection: ServerConnection, info: WriterInfo) extends PacketWriter {

    override val identifier       : Int           = info.identifier
    override val traffic          : PacketTraffic = info.traffic
    override val serverIdentifier : String        = serverConnection.supportIdentifier
    override val supportIdentifier: String        = traffic.supportIdentifier

    //private val notifier = info.notifier
    //private val hooks = info.packetHooks

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        targetIDs.foreach(targetID => {
            /*
             * If the targetID is the same as the server's identifier, that means that we target ourself,
             * so the packet, as it can't be written to a socket that target the current server, will be directly
             * injected into the traffic.
             * */
            if (targetID == serverIdentifier) {
                val coords = DedicatedPacketCoordinates(identifier, targetID, serverIdentifier)
                //val event = PacketEvents.packedSent(packet, coords)
                //notifier.notifyEvent(hooks, event)
                traffic.handleInjection(PacketInjections.unhandled(coords, packet))
                return
            }
            val opt = serverConnection.getConnection(targetID)
            if (opt.isDefined) {
                opt.get.sendPacket(packet, identifier)
            } else {
                throw NoSuchConnectionException(s"Attempted to send a packet to target $targetID, but this conection is missing or not connected.")
            }
        })
    }

    override def writeBroadcastPacket(packet: Packet, discarded: String*): Unit = {
        serverConnection.broadcastPacketToConnections(packet, supportIdentifier, identifier, discarded: _*)
    }
}
