/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.persistence

import fr.linkit.api.connection.cache.repo.InvocationChoreographer
import fr.linkit.api.connection.packet.persistence.{Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet) extends TransferInfo {

    override def makeSerial(serializer: Serializer, buff: ByteBuffer): Unit = {
        val packetBuff = ArrayBuffer.empty[Any]
        packetBuff += coords
        if (attributes.nonEmpty)
            packetBuff += attributes
        if (packet != EmptyPacket)
            packetBuff += packet
        InvocationChoreographer.forceLocalInvocation {
            AppLogger.debug(s"$currentTasksId <> Making simple serialize $buff...")
        }
        serializer.serialize(packetBuff.toArray[Any], buff, true)
    }
}
