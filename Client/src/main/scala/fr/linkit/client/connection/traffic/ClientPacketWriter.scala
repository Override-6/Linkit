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

package fr.linkit.client.connection.traffic

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.packet.AbstractPacketWriter
import fr.linkit.engine.gnom.packet.traffic.{DynamicSocket, WriterInfo}
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import fr.linkit.engine.internal.util.OrdinalCounter

class ClientPacketWriter(socket: DynamicSocket,
                         ordinal: OrdinalCounter,
                         translator: ObjectTranslator,
                         writerInfo: WriterInfo) extends AbstractPacketWriter {

    override      val traffic          : PacketTraffic     = writerInfo.traffic
    override      val currentEngineName: String            = traffic.currentEngineName
    override      val serverName       : String            = traffic.serverName
    override      val path             : Array[Int]        = writerInfo.path
    private       val persistenceConfig: PersistenceConfig = writerInfo.persistenceConfig
    override lazy val network                              = writerInfo.network

    protected def writePacketsInclude(packet: Packet, attributes: PacketAttributes, includedTags: Array[String]): Unit = {
        if (includedTags.length == 1) {
            val target    = includedTags.head
            val dedicated = DedicatedPacketCoordinates(path, target, currentEngineName)
            if (target == currentEngineName) {
                traffic.processInjection(packet, attributes, dedicated)
                return
            }
            send(dedicated)(attributes, packet)
        } else {
            if (includedTags.contains(currentEngineName)) {
                val coords = DedicatedPacketCoordinates(path, serverName, currentEngineName)
                traffic.processInjection(packet, attributes, coords)
            }

            for (target <- includedTags) if (target != currentEngineName) {
                val coords = DedicatedPacketCoordinates(path, target, currentEngineName)
                send(coords)(attributes, packet)
            }
        }
    }

    protected def writePacketsExclude(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit = {
        if (!discardedIDs.contains(currentEngineName))
            traffic.processInjection(packet, attributes, DedicatedPacketCoordinates(path, currentEngineName, currentEngineName))
        network.listEngines
                .filterNot(e => discardedIDs.contains(e.name))
                .foreach(engine => {
                    val coords = DedicatedPacketCoordinates(path, engine.name, currentEngineName)
                    send(coords)(attributes, packet)
                })
    }

    private def send(coords: DedicatedPacketCoordinates)(attributes: PacketAttributes, packet: Packet): Unit = {
        val transferInfo = SimpleTransferInfo(coords, attributes, packet, persistenceConfig, network)
        val target       = coords.targetID
        val result       = translator.translate(transferInfo)
        socket.write(result.buff(() => this.ordinal.increment(target)))
    }
}