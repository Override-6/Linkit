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

package fr.linkit.api.connection.packet.serialization

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategyHolder}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}

trait PacketTranslator extends StrategyHolder {

    def translate(packetInfo: TransferInfo): PacketSerializationResult

    def translate(bytes: Array[Byte]): PacketTransferResult

    def translateCoords(coords: PacketCoordinates, target: String): Array[Byte]

    def translateAttributes(attribute: PacketAttributes, target: String): Array[Byte]

    def translatePacket(packet: Packet, target: String): Array[Byte]

    def updateCache(manager: SharedCacheManager): Unit

    def attachStrategy(strategy: SerialStrategy[_]): Unit

    def findSerializerFor(target: String): Option[Serializer]

    val signature: Array[Byte]

}
