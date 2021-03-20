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

import fr.`override`.linkit.server.connection.ConnectionException

class ServerPacketWriter(server: RelayServer, info: WriterInfo) extends PacketWriter {

    override val identifier: Int = info.identifier
    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID
    override val ownerID: String = traffic.ownerID

    private val notifier = info.notifier
    private val hooks = info.packetHooks

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        targetIDs.foreach(targetID => {
            if (targetID == server.identifier) {
                val coords = DedicatedPacketCoordinates(identifier, targetID, relayID)
                val event = PacketEvents.packedSent(packet, coords)
                notifier.notifyEvent(hooks, event)
                traffic.handleInjection(PacketInjections.unhandled(coords, packet))
                return
            }
            if (server.isConnected(targetID)) {
                server.getConnection(targetID).get.sendPacket(packet, identifier)

            } else {
                throw ConnectionException(s"Attempted to send a packet to target $targetID, but this target is not connected.")
            }
        })
    }

    override def writeBroadcastPacket(packet: Packet, discarded: String*): Unit = {
        server.broadcastPacketToConnections(packet, ownerID, identifier, discarded: _*)
    }
}
