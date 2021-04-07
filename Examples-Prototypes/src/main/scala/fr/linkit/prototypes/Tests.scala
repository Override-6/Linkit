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

package fr.linkit.prototypes

import fr.linkit.api.connection.network.ExternalConnectionState
import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.ArrayRefPacket
import fr.linkit.core.connection.packet.serialization.{LocalCachedObjectSerializer, ObjectSerializer, SimpleTransferInfo}
import fr.linkit.core.connection.packet.serialization.strategies.PacketAttributesStrategy
import fr.linkit.core.connection.packet.traffic.channel.request.ResponsePacket

object Tests {

    import ExternalConnectionState._

    private val coords     = DedicatedPacketCoordinates(12, "s1", "TestServer1")
    private val packet     = ResponsePacket(6, Array(ArrayRefPacket[ExternalConnectionState](Array(CONNECTED))))
    private val attributes = SimplePacketAttributes.empty

    def main(args: Array[String]): Unit = {
        val serialInfo = SimpleTransferInfo(coords, attributes, packet)

        println("")
        println("-- Compact serializer --")
        println("")

        //COMPACTED SERIALIZER
        {
            val translator   = new LocalCompactTranslator()
            val serialResult = translator.translate(serialInfo)

            val bytes = serialResult.bytes
            println("Compacted bytes                 = " + new String(bytes) + (s" (l: ${bytes.length})"))

            val result = translator.translate(bytes)
            println()
            println(s"Deserialized Info = (${result.coords}, ${result.attributes}, ${result.packet})")

        }
        println("")
        println("")
        println("")

        {
            val bytes = LocalCachedObjectSerializer.serialize(Array(coords, attributes, packet), true)
            println(s"new String(bytes) = ${new String(bytes)}")

            val result = LocalCachedObjectSerializer.deserializeAll(bytes)
            println(s"result = ${result.mkString("Array(", ", ", ")")}")
        }

    }

}
