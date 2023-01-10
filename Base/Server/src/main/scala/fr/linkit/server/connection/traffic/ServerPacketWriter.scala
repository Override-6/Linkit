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
import fr.linkit.api.gnom.network.tag.{EngineSelector, NameTag, Server}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.engine.gnom.packet.AbstractPacketWriter
import fr.linkit.engine.gnom.packet.traffic.WriterInfo
import fr.linkit.server.connection.ServerConnection

class ServerPacketWriter(serverConnection: ServerConnection,
                         info            : WriterInfo) extends AbstractPacketWriter {

    override  val path    : Array[Int]     = info.path
    override  val traffic : PacketTraffic  = info.traffic
    override val selector: EngineSelector = info.network

    override protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Seq[NameTag]): Unit = {
        targets.foreach(targetTag => {
            /*
             * If the targetID is the same as the server's identifier, that means that we target ourself,
             * so the packet, as it can't be written to a socket that target the current server, will be directly
             * injected into the traffic.
             * */
            if (selector.isEquivalent(targetTag, Server)) {
                val coords = DedicatedPacketCoordinates(path, targetTag, targetTag) //target IS server.
                traffic.processInjection(packet, attributes, coords)
                return
            }
            val opt = serverConnection.getConnection(targetTag)
            if (opt.isDefined) {
                opt.get.sendPacket(packet, attributes, path)
            } else {
                throw NoSuchConnectionException(s"Attempted to send a packet to target '$targetTag', but this conection is missing or not connected.")
            }
        })
    }

}

