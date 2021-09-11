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
import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.persistence.context.{DefaultPersistenceContext, SimplePacketConfig}
import fr.linkit.engine.connection.packet.persistence.serializor.DefaultPacketSerializer
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacket
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.utils.ScalaUtils
import fr.linkit.engine.test.PacketTests.{serializer, testPacket}
import fr.linkit.engine.test.classes.Player
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeAll, RepeatedTest, Test, TestInstance}

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
        val f = Player(45, "Test", "Test", 78, 78)
        testPacket(Array(f, f, f))
    }


    @RepeatedTest(5)
    def perfTests(): Unit = {
        val f = ArrayBuffer(DedicatedPacketCoordinates(Array.empty, "TestServer1", "s1"), SimplePacketAttributes("family" -> "Global Cache", "behavior" -> "GET_OR_OPEN"), RequestPacket(1, Array(IntPacket(3))))
        val obj = Array[AnyRef](f, f, f)
        val coords = DedicatedPacketCoordinates(Array.empty, "SALAM", "SALAM")
        val buff   = ByteBuffer.allocate(1000)
        val config = new SimplePacketConfig {}
        val t0 = System.currentTimeMillis()
        for (_ <- 0 to 1000) {
            serializer.serializePacket(obj, coords, buff)(config)
            buff.position(0)
            serializer.deserializePacket(buff)(config)
                    .forEachObjects(() => _)
        }
        val t1 = System.currentTimeMillis()
        println(t1-t0)
    }

    @Test
    def moreComplexPacketTest(): Unit = {
        testPacket(Array[AnyRef](Array[Int](0, -1598464148)))
    }

    @Test
    def complexPacketTest(): Unit = {
        val packet = ArrayBuffer(DedicatedPacketCoordinates(Array.empty, "TestServer1", "s1"), SimplePacketAttributes("family" -> "Global Cache", "behavior" -> "GET_OR_OPEN"), RequestPacket(1, Array(IntPacket(3))))
        testPacket(Array(packet))
    }

}

object PacketTests {

    private val serializer = new DefaultPacketSerializer(null, new DefaultPersistenceContext)

    def testPacket(obj: Array[AnyRef]): Unit = {
        println(s"Serializing packets ${obj.mkString("Array(", ", ", ")")}...")
        val buff = ByteBuffer.allocate(1000)
        val config =  new SimplePacketConfig {}
        serializer.serializePacket(obj, DedicatedPacketCoordinates(Array.empty, "SALAM", "SALAM"), buff)(config)
        val bytes = buff.array().take(buff.position())
        buff.position(0)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (size: ${bytes.length})")
        val deserial = serializer.deserializePacket(buff)(config)
        println(s"deserialized coords = ${deserial.getCoordinates}")
        deserial.forEachObjects(packet2 => {
            println(s"deserialized packet = ${packet2}")
        })
    }

}
