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

package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.serialization.{PacketDeserializationResult, PacketTranslator}
import fr.`override`.linkit.api.connection.packet.traffic.PacketReader
import fr.`override`.linkit.api.local.system.security.BytesHasher

class DefaultPacketReader(socket: DynamicSocket,
                          hasher: BytesHasher,
                          translator: PacketTranslator) extends PacketReader {

    private var packetCount = 0

    /**
     * @return a tuple containing the next packet with its coordinates and its local number identifier
     * */
    override def nextPacket(callback: (PacketDeserializationResult, Int) => Unit): Unit = {
        println("Hello ?")
        val nextLength = socket.readInt()
        println(s"nextLength = ${nextLength}")
        println(s"socket.isOpen = ${socket.isOpen}")
        if (nextLength == -1 || !socket.isOpen)
            return

        println("A")
        val bytes = hasher.deHashBytes(socket.read(nextLength))
        println("B")
        val result = translator.translate(bytes)
        println("C")
        packetCount += 1
        val currentPacketNumber = packetCount
        println("D")
        callback(result, currentPacketNumber)
        println("E")
    }
}
