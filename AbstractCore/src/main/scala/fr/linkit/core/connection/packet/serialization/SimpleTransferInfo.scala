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

import fr.linkit.api.connection.packet.serialization.{Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet) extends TransferInfo {

    override def makeSerial(serializer: Serializer): Array[Byte] = {
        val buff = ArrayBuffer.empty[Serializable]
        buff += coords
        if (attributes.nonEmpty)
            buff += attributes
        if (packet != EmptyPacket)
            buff += packet
        AppLogger.vError(s"Making simple serialize $buff")
        serializer.serialize(buff.toArray, true)
    }
}
