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

package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.traffic.{PacketInjections, PacketTraffic, PacketWriter, WriterInfo}
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.server.exceptions.ConnectionException

class ServerPacketWriter(server: RelayServer, info: WriterInfo) extends PacketWriter {

    override val identifier: Int = info.identifier
    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID
    override val ownerID: String = traffic.ownerID

    override def writePacket(packet: Packet, targetIDs: String*): Unit = targetIDs.foreach(targetID => {
        if (targetID == server.identifier) {
            traffic.handleInjection(PacketInjections.unhandled(DedicatedPacketCoordinates(identifier, targetID, relayID), packet))
            return
        }
        //println(s"WRITING PACKETS $packet TO $targetID")
        if (server.isConnected(targetID)) {
            server.getConnection(targetID).sendPacket(packet, identifier)
        } else {
            throw ConnectionException(s"Attempted to send a packet to target $targetID, but this target is not connected.")
        }
    })

    override def writeBroadcastPacket(packet: Packet, discarded: String*): Unit = {
        server.broadcastPacketToConnections(packet, ownerID, identifier, discarded: _*)
    }
}
