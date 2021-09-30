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

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.{ObjectPersistence, TransferInfo}
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.persistence.context.{ImmutablePersistenceContext, PersistenceConfigBuilder, SimplePersistenceConfig}
import fr.linkit.engine.internal.utils.ClassMap

import java.io.File
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet,
                              config: PersistenceConfig) extends TransferInfo {

    override def makeSerial(serializer: ObjectPersistence, buff: ByteBuffer): Unit = {
        val packetBuff = ArrayBuffer.empty[AnyRef]
        if (attributes.nonEmpty)
            packetBuff += attributes
        if (packet != EmptyPacket)
            packetBuff += packet
        InvocationChoreographer.forceLocalInvocation {
            AppLogger.debug(s"$currentTasksId <> Making simple serialize $coords, $packetBuff...")
        }
        val content = packetBuff.toArray[AnyRef]
        serializer.serializePacket(content, coords, buff)(config)
    }
}
