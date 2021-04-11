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

package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{PacketSerializationResult, Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}

case class LazyPacketSerializationResult(info: TransferInfo,
                                         private val serializer: () => Serializer) extends PacketSerializationResult {

    override val coords: PacketCoordinates = info.coords

    override val attributes: PacketAttributes = info.attributes

    override val packet: Packet = info.packet

    override lazy val bytes: Array[Byte] = info.makeSerial(serializer.apply())

    override lazy val writableBytes: Array[Byte] = {
        val length = bytes.length
        Array[Byte](
            ((length >> 24) & 0xff).toByte,
            ((length >> 16) & 0xff).toByte,
            ((length >> 8) & 0xff).toByte,
            ((length >> 0) & 0xff).toByte
        ) ++ bytes
    }

    override def getSerializer: Serializer = this.serializer.apply()

}

object LazyPacketSerializationResult {

    implicit def autoUseWritableBytes(result: PacketSerializationResult): Array[Byte] = {
        result.writableBytes
    }
}