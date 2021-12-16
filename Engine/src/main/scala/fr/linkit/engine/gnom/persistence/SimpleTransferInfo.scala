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

import java.nio.ByteBuffer

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PersistenceBundle, TransferInfo}
import fr.linkit.api.gnom.reference.linker.GeneralNetworkObjectLinker
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket

import scala.collection.mutable.ArrayBuffer

case class SimpleTransferInfo(override val coords: DedicatedPacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet,
                              config: PersistenceConfig,
                              network: Network) extends TransferInfo {
    info =>

    override def makeSerial(serializer: ObjectPersistence, buffer: ByteBuffer): Unit = {
        val packetBuff = ArrayBuffer.empty[AnyRef]
        if (attributes.nonEmpty)
            packetBuff += attributes
        if (packet != EmptyPacket)
            packetBuff += packet
        val content = packetBuff.toArray[AnyRef]
        serializer.serializeObjects(content)(new PersistenceBundle {
            override val buff        : ByteBuffer                 = buffer
            override val boundId     : String                     = coords.targetID
            override val packetPath  : Array[Int]                 = coords.path
            override val config      : PersistenceConfig          = info.config
            override val gnol        : GeneralNetworkObjectLinker = network.gnol
        })
    }
}
