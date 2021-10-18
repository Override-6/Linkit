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

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PersistenceBundle, TransferInfo}
import fr.linkit.api.gnom.reference.GeneralNetworkObjectLinker
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet,
                              config: PersistenceConfig,
                              gnol: GeneralNetworkObjectLinker) extends TransferInfo { info =>

    override def makeSerial(serializer: ObjectPersistence, b: ByteBuffer): Unit = {
        val packetBuff = ArrayBuffer.empty[AnyRef]
        if (attributes.nonEmpty)
            packetBuff += attributes
        if (packet != EmptyPacket)
            packetBuff += packet
        InvocationChoreographer.forceLocalInvocation {
            AppLogger.debug(s"$currentTasksId <> Making simple serialize $coords, $packetBuff...")
        }
        val content = packetBuff.toArray[AnyRef]
        serializer.serializeObjects(content)(new PersistenceBundle {
            override val buff       : ByteBuffer                 = b
            override val coordinates: PacketCoordinates          = coords
            override val config     : PersistenceConfig          = info.config
            override val gnol       : GeneralNetworkObjectLinker = info.gnol
        })
    }
}
