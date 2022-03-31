/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.persistence.{PacketSerializationChoreographer, SimpleTransferInfo}

class SocketPacketWriter(socket: DynamicSocket,
                         choreographer: PacketSerializationChoreographer,
                         writerInfo: WriterInfo) extends PacketWriter {

    override     val traffic          : PacketTraffic     = writerInfo.traffic
    override     val serverIdentifier : String            = traffic.serverIdentifier
    override     val currentIdentifier: String            = traffic.currentIdentifier
    override     val path             : Array[Int]        = writerInfo.path
    private      val persistenceConfig: PersistenceConfig = writerInfo.persistenceConfig
    private lazy val network                              = writerInfo.network

    /*
    * Incremented each time a packet is written in the socket.
    * */
    private var packetOrdinal = 0

    private def nextOrdinal: Int = {
        packetOrdinal += 1
        packetOrdinal
    }

    override def writePacket(packet: Packet, targetIDs: Array[String]): Unit = {
        writePacket(packet, SimplePacketAttributes.empty, targetIDs)
    }

    override def writePacket(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit = {
        val packetOrdinal = nextOrdinal
        if (targetIDs.length == 1) {
            val target    = targetIDs.head
            val dedicated = DedicatedPacketCoordinates(path, target, currentIdentifier, packetOrdinal)
            if (target == currentIdentifier) {
                traffic.processInjection(packet, attributes, dedicated)
                return
            }
            addToChoreographer(dedicated)(attributes, packet)
        } else {
            if (targetIDs.contains(currentIdentifier)) {
                val coords = DedicatedPacketCoordinates(path, serverIdentifier, currentIdentifier, packetOrdinal)
                traffic.processInjection(packet, attributes, coords)
            }

            for (target <- targetIDs) if (target != currentIdentifier) {
                val coords = DedicatedPacketCoordinates(path, target, currentIdentifier, packetOrdinal)
                addToChoreographer(coords)(attributes, packet)
            }
        }
    }

    override def writeBroadcastPacket(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit = {
        val packetOrdinal = nextOrdinal
        if (!discardedIDs.contains(currentIdentifier))
            traffic.processInjection(packet, attributes, DedicatedPacketCoordinates(path, currentIdentifier, currentIdentifier, packetOrdinal))
        network.listEngines
            .filterNot(e => discardedIDs.contains(e.identifier))
            .foreach(engine => {
                val coords = DedicatedPacketCoordinates(path, engine.identifier, currentIdentifier, packetOrdinal)
                addToChoreographer(coords)(attributes, packet)
            })
    }

    private def addToChoreographer(coords: DedicatedPacketCoordinates)(attributes: PacketAttributes, packet: Packet): Unit = {
        val transferInfo = SimpleTransferInfo(coords, attributes, packet, persistenceConfig, network)
        choreographer.add(transferInfo)(result => socket.write(result.buff))
    }

    override def writeBroadcastPacket(packet: Packet, discardedIDs: Array[String]): Unit = {
        writeBroadcastPacket(packet, SimplePacketAttributes.empty, discardedIDs)
    }

}