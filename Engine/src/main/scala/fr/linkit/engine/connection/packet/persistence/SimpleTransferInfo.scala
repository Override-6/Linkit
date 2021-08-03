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

import fr.linkit.api.connection.cache.obj.InvocationChoreographer
import fr.linkit.api.connection.packet.persistence.{PacketSerializer, Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket
import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet) extends TransferInfo {

    override def makeSerial(serializer: PacketSerializer, buff: ByteBuffer): Unit = {
        val packetBuff = ArrayBuffer.empty[AnyRef]
        if (attributes.nonEmpty)
            packetBuff += attributes
        if (packet != EmptyPacket)
            packetBuff += packet
        InvocationChoreographer.forceLocalInvocation {
            AppLogger.debug(s"$currentTasksId <> Making simple serialize $coords, $packetBuff...")
        }
        serializer.serializePacket(packetBuff.toArray[AnyRef], coords, buff, true)
    }
}