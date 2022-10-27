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

package fr.linkit.server.connection.traffic

import fr.linkit.api.application.connection.NoSuchConnectionException
import fr.linkit.api.gnom.network.{EngineTag, Network}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.traffic.WriterInfo
import fr.linkit.engine.gnom.packet.{AbstractPacketWriter, SimplePacketAttributes}
import fr.linkit.server.connection.ServerConnection

class ServerPacketWriter(serverConnection: ServerConnection,
                         info: WriterInfo) extends AbstractPacketWriter {

    override val path             : Array[Int]    = info.path
    override val traffic          : PacketTraffic = info.traffic
    override val serverName       : String        = serverConnection.currentIdentifier
    override val currentEngineName: String        = traffic.currentEngineName

    override protected def network: Network = serverConnection.network

    override def writePackets(packet: Packet, targetTags: Array[EngineTag], excludeTargets: Boolean): Unit = {
        writePackets(packet, SimplePacketAttributes.empty, targetTags, excludeTargets)
    }

    override protected def writePacketsInclude(packet: Packet, attributes: PacketAttributes, includedTags: Array[String]): Unit = {
        includedTags.foreach(targetID => {
            /*
             * If the targetID is the same as the server's identifier, that means that we target ourself,
             * so the packet, as it can't be written to a socket that target the current server, will be directly
             * injected into the traffic.
             * */
            if (targetID == serverName) {
                val coords = DedicatedPacketCoordinates(path, targetID, serverName)
                traffic.processInjection(packet, attributes, coords)
                return
            }
            val opt = serverConnection.getConnection(targetID)
            if (opt.isDefined) {
                opt.get.sendPacket(packet, attributes, path)
            } else {
                throw NoSuchConnectionException(s"Attempted to send a packet to target '$targetID', but this conection is missing or not connected.")
            }
        })
    }

    protected def writePacketsExclude(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit = {
        serverConnection.broadcastPacket(packet, attributes, currentEngineName, path, info.persistenceConfig, discardedIDs)
    }

}

