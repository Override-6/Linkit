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

import com.google.gson.Gson
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, PacketInfo}
import fr.linkit.api.local.system.security.BytesHasher
import fr.linkit.core.connection.packet.fundamental.RefPacket.{AnyRefPacket, StringPacket}
import fr.linkit.core.connection.packet.fundamental.ValPacket.LongPacket
import fr.linkit.core.connection.packet.fundamental.{EmptyPacket, WrappedPacket}
import fr.linkit.core.connection.packet.serialization.strategies.MapStrategy
import fr.linkit.core.connection.packet.serialization.{AdaptivePacketTranslator, PartialTransferInfo, SimpleTransferInfo}
import fr.linkit.core.connection.packet.{EmptyPacketAttributes, SimplePacketAttributes}

import java.io.{ByteArrayOutputStream, ObjectOutputStream}

object Tests {

    private val coords     = DedicatedPacketCoordinates(27, "server", "client")
    private val packet     = WrappedPacket("Hello", WrappedPacket("World", AnyRefPacket(("Damn", "How", "Are", WrappedPacket("You ?", LongPacket(8)), EmptyPacket))))
    private val attributes = SimplePacketAttributes.from("test1" -> "wew", "test2" -> StringPacket("wuw"), "test3" -> 4)

    def main(args: Array[String]): Unit = {
        val serialInfo = SimpleTransferInfo(coords, EmptyPacketAttributes, packet)

        println("")
        println("-- Compact serializer --")
        println("")

        //COMPACTED SERIALIZER
        {
            val translator   = new LocalCompactTranslator()
            val partialInfo  = PartialTransferInfo((coords, translator.translateCoords(coords)), (attributes, translator.translateAttributes(attributes)), (packet, null))
            val serialResult = translator.translate(partialInfo)

            val bytes        = serialResult.bytes
            println("Compacted bytes                 = " + new String(bytes) + (s" (l: ${bytes.length})"))

            MapStrategy.attachDefaultStrategies(translator)
            val bytesStrategy        = serialResult.bytes
            println("Compacted bytes (with strategy) = " + new String(bytesStrategy) + (s" (l: ${bytesStrategy.length})"))

            val result = translator.translate(bytes)
            println(s"Deserialized Info = ${PacketInfo(result)}")
        }

        println("")
        println("-- Raw serializer --")
        println("")

        //RAW SERIALIZER
        {
            val translator   = new AdaptivePacketTranslator("owner", BytesHasher.inactive)
            val serialResult = translator.translate(serialInfo)
            val bytes        = serialResult.bytes
            println("Compacted bytes = " + new String(bytes) + (s" (l: ${bytes.length})"))
            val result = translator.translate(bytes)
            println(s"Deserialized Info = ${PacketInfo(result)}")
        }

        println("")
        println("-- Gson serializer --")
        println("")

        //GSON
        {
            val gson  = new Gson()
            val bytes = gson.toJson(Array(coords, attributes, packet))
            println("Json bytes = " + new String(bytes) + (s" (l: ${bytes.length})"))
        }

        println("")
        println("-- Java serializer --")
        println("")

        //JAVA
        {
            val baos = new ByteArrayOutputStream()
            val out  = new ObjectOutputStream(baos)
            out.writeObject(Array(coords, attributes, packet))
            val bytes = baos.toByteArray
            println("Java bytes = " + new String(bytes) + (s" (l: ${bytes.length})"))
        }
    }

}
