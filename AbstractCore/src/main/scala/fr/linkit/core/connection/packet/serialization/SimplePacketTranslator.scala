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

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.serialization._
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.security.BytesHasher

class SimplePacketTranslator(hasher: BytesHasher) extends PacketTranslator {

    override def translate(packetInfo: TransferInfo): PacketSerializationResult = {
        new LazyPacketSerializationResult(packetInfo, () => DefaultSerializer)
    }

    //TODO Create trait named "PacketDeserializationResult" even if it is empty.
    override def translate(bytes: Array[Byte]): PacketTransferResult = {
        new LazyPacketDeserializationResult(bytes, () => DefaultSerializer)
    }

    override def translateCoords(coords: PacketCoordinates, target: String): Array[Byte] = {
        DefaultSerializer.serialize(coords, false)
    }

    override def translateAttributes(attribute: PacketAttributes, target: String): Array[Byte] = {
        DefaultSerializer.serialize(attribute, false)
    }

    override def translatePacket(packet: Packet, target: String): Array[Byte] = {
        DefaultSerializer.serialize(packet, false)
    }

    override def updateCache(manager: SharedCacheManager): Unit = ()

    override def findSerializerFor(target: String): Option[Serializer] = Some(DefaultSerializer)

    override val signature: Array[Byte] = DefaultSerializer.signature
}
