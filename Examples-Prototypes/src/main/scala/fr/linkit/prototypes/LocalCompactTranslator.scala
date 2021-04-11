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

package fr.linkit.prototypes

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategyHolder}
import fr.linkit.api.connection.packet.serialization._
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.serialization.{LazyPacketDeserializationResult, LazyPacketSerializationResult, LocalCachedObjectSerializer}

class LocalCompactTranslator extends PacketTranslator {
    override def translate(packetInfo: TransferInfo): PacketSerializationResult = {
        LazyPacketSerializationResult(packetInfo, () => LocalCachedObjectSerializer)
    }

    override def translate(bytes: Array[Byte]): PacketTransferResult = {
        LazyPacketDeserializationResult(bytes, () => LocalCachedObjectSerializer)
    }

    override def translateCoords(coords: PacketCoordinates, target: String = ""): Array[Byte] = {
        LocalCachedObjectSerializer.serialize(coords, false)
    }

    override def translateAttributes(attribute: PacketAttributes, target: String = ""): Array[Byte] = {
        LocalCachedObjectSerializer.serialize(attribute, false)
    }

    override def translatePacket(packet: Packet, target: String = ""): Array[Byte] = {
        LocalCachedObjectSerializer.serialize(packet, false)
    }

    override def updateCache(manager: SharedCacheManager): Unit = ()

    override val signature: Array[Byte] = Array[Byte](5)

    override def attachStrategy(strategy: SerialStrategy[_]): Unit = LocalCachedObjectSerializer.attachStrategy(strategy)

    override def drainAllStrategies(holder: StrategyHolder): Unit = LocalCachedObjectSerializer.drainAllStrategies(holder)

    override def findSerializerFor(target: String): Option[Serializer] = Some(LocalCachedObjectSerializer)
}
