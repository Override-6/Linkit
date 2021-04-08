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

import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.api.local.ApplicationContext
import fr.linkit.core.connection.network.AbstractNetwork
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.ArrayRefPacket
import fr.linkit.core.connection.packet.serialization.HashCodeTypesObjectSerializer
import fr.linkit.core.connection.packet.traffic.channel.request.ResponsePacket
import fr.linkit.core.local.mapping.ClassMapEngine
import fr.linkit.core.local.system.fsa.JDKFileSystemAdapters

import java.sql.Timestamp
import java.time.Instant

object Tests {

    private val coords     = DedicatedPacketCoordinates(12, "s1", "TestServer1")
    private val packet     = ResponsePacket(6, Array(ArrayRefPacket(Array(2, Timestamp.from(Instant.now())))))
    private val attributes = SimplePacketAttributes.empty

    private val fsa = JDKFileSystemAdapters.Nio

    def main(args: Array[String]): Unit = {
        /*println("Performing Linkit classes mapping...")
        ClassMapEngine.mapAllSourcesOfClasses(fsa, getClass, classOf[ApplicationContext], Predef.getClass, classOf[AbstractNetwork])
        println("Performing JDK classes mapping...")
        ClassMapEngine.mapJDK(fsa)

        val bytes = HashCodeTypesObjectSerializer.serialize(Array(coords, packet, attributes), true)
        println(s"new String(bytes) = ${new String(bytes)} (l: ${bytes.length}) ")
        val obj = HashCodeTypesObjectSerializer.deserializeAll(bytes)
        println(s"obj = ${obj.mkString("Array(", ", ", ")")}")*/
    }

}
