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

package fr.`override`.linkit.api.connection.packet.serialization

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

case class PacketSerializationResult(packet: Packet, coords: PacketCoordinates, serializer: () => Serializer) {

    lazy val bytes: Array[Byte] = serializer().serialize(Array(coords, packet))

    def writableBytes: Array[Byte] = {
        val length = bytes.length
        Array[Byte](
            ((length >> 24) & 0xff).toByte,
            ((length >> 16) & 0xff).toByte,
            ((length >> 8) & 0xff).toByte,
            ((length >> 0) & 0xff).toByte
        ) ++ bytes
    }

}

object PacketSerializationResult {
    implicit def autoUseWritableBytes(result: PacketSerializationResult): Array[Byte] = {
        result.writableBytes
    }
}