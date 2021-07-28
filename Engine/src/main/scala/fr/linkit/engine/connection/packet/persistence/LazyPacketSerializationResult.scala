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

import java.nio.ByteBuffer

import fr.linkit.api.connection.packet.persistence.{PacketSerializationResult, PacketSerializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}

case class LazyPacketSerializationResult(info: TransferInfo,
                                         private val serializer: PacketSerializer) extends PacketSerializationResult {

    override val coords: PacketCoordinates = info.coords

    override val attributes: PacketAttributes = info.attributes

    override val packet: Packet = info.packet

    override lazy val buff: ByteBuffer = {
        val buff = ByteBuffer.allocateDirect(10000)
        buff.position(4)
        info.makeSerial(serializer, buff)
        buff.putInt(0, buff.position() - 4) //write the packet's length
        buff.flip()
    }

}

object LazyPacketSerializationResult {

}