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

package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.api.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet}

class ServerSharedCacheHandler(family: String, traffic: PacketTraffic) extends SharedCacheHandler(family, traffic) {
    override def continuePacketHandling(packet: Packet, coords: DedicatedPacketCoordinates): Unit = packet match {
        case LongPacket(cacheID) =>
            val senderID: String = coords.senderID
            println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
            val content = LocalCacheHandler.getContentOrElseMock(cacheID)
            println(s"Content = ${content.mkString("Array(", ", ", ")")}")
            communicator.sendResponse(ArrayObjectPacket(content), senderID)
            println("Packet sent :D")
    }

}

object ServerSharedCacheHandler {
    def apply(): (String, PacketTraffic) => ServerSharedCacheHandler = new ServerSharedCacheHandler(_, _)
}
