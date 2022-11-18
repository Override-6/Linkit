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

import fr.linkit.api.gnom.network.tag.{Current, NameTag, Server}
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.packet.AbstractPacketWriter
import fr.linkit.engine.gnom.packet.traffic.{DynamicSocket, WriterInfo}
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import fr.linkit.engine.internal.util.OrdinalCounter

class ClientPacketWriter(socket    : DynamicSocket,
                         ordinal   : OrdinalCounter,
                         translator: ObjectTranslator,
                         writerInfo: WriterInfo) extends AbstractPacketWriter {

    override      val traffic          : PacketTraffic     = writerInfo.traffic
    override      val path             : Array[Int]        = writerInfo.path
    private       val persistenceConfig: PersistenceConfig = writerInfo.persistenceConfig
    override lazy val selector                             = writerInfo.network

    private lazy val serverNT : NameTag = selector.retrieveNT(Server)
    private lazy val currentNT: NameTag = selector.retrieveNT(Current)

    override protected def writePackets(packet: Packet, attributes: PacketAttributes, targets: Seq[NameTag]): Unit = {
        if (targets.length == 1) {
            val target    = targets.head
            val dedicated = DedicatedPacketCoordinates(path, target, currentNT)
            if (target == currentNT) {
                traffic.processInjection(packet, attributes, dedicated)
                return
            }
            send(dedicated)(attributes, packet)
        } else {
            if (targets.contains(currentNT)) {
                val coords = DedicatedPacketCoordinates(path, serverNT, currentNT)
                traffic.processInjection(packet, attributes, coords)
            }

            for (target <- targets) if (target != currentNT) {
                val coords = DedicatedPacketCoordinates(path, target, currentNT)
                send(coords)(attributes, packet)
            }
        }
    }

    private def send(coords: DedicatedPacketCoordinates)(attributes: PacketAttributes, packet: Packet): Unit = {
        val transferInfo = SimpleTransferInfo(coords, attributes, packet, persistenceConfig, selector)
        val target       = coords.targetNT
        val result       = translator.translate(transferInfo)
        socket.write(result.buff(() => this.ordinal.increment(target)))
    }
}