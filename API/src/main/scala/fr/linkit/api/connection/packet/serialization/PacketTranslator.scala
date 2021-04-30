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

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}

trait PacketTranslator {

    def translate(packetInfo: TransferInfo): PacketSerializationResult

    def translate(bytes: Array[Byte]): PacketTransferResult

    def translateCoords(coords: PacketCoordinates, target: String): Array[Byte]

    def translateAttributes(attribute: PacketAttributes, target: String): Array[Byte]

    def translatePacket(packet: Packet, target: String): Array[Byte]

    def initNetwork(network: Network): Unit

    def getSerializer: Serializer

    val signature: Array[Byte]

}
