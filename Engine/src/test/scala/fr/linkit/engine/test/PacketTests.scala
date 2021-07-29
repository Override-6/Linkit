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

package fr.linkit.engine.test

import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.persistence.DefaultPacketSerializer
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacket
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.utils.ScalaUtils
import fr.linkit.engine.test.PacketTests.testPacket
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeAll, Test, TestInstance}

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

@TestInstance(Lifecycle.PER_CLASS)
class PacketTests {

    @BeforeAll
    def makeMapping(): Unit = {
        LinkitApplication.mapEnvironment(LocalFileSystemAdapters.Nio, Seq(getClass))
    }

    @Test
    def simplePacketTest(): Unit = {
        testPacket(Array(AnyRefPacket(None)))
    }

    object EmptyObject {

    }

    @Test
    def moreComplexPacketTest(): Unit = {
        testPacket(Array[AnyRef](Array(1, 2f, 3d, 4.0f, 3.0d, 100.004321f, 156321d, 456.786541d)))
    }

    @Test
    def complexPacketTest(): Unit = {
        val packet = ArrayBuffer(DedicatedPacketCoordinates(12, "TestServer1", "s1"), SimplePacketAttributes("family" -> "Global Cache", "behavior" -> "GET_OR_OPEN"), RequestPacket(1, Array(IntPacket(3))))
        testPacket(Array(packet))
    }

}

object PacketTests {

    private val serializer = new DefaultPacketSerializer
    serializer.context.addPersistence(new PuppetWrapperPersistor(null))

    def testPacket(obj: Array[AnyRef]): Unit = {
        println(s"Serializing packets ${obj.mkString("Array(", ", ", ")")}...")
        val buff = ByteBuffer.allocate(1000)
        serializer.serializePacket(obj, DedicatedPacketCoordinates(78, "SALAM", "SALAM"), buff, true)
        val bytes = buff.array().take(buff.position())
        buff.position(0)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (size: ${bytes.length})")
        val deserial = serializer.deserializePacket(buff)
        println(s"deserialized coords = ${deserial.getCoordinates}")
        deserial.forEachObjects(packet2 => {
            println(s"deserialized packet = ${packet2}")
        })
    }

}
