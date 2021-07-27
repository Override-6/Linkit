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

import fr.linkit.api.connection.cache.CacheSearchBehavior
import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.engine.connection.cache.repo.CacheRepoContent
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter.PuppetProfile
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.persistence.DefaultSerializer
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacket
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.utils.{PerformanceMeter, ScalaUtils}
import fr.linkit.engine.test.PacketTests.testPacket
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeAll, Test, TestInstance}

import java.util
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

@TestInstance(Lifecycle.PER_CLASS)
class PacketTests {

    @BeforeAll
    def makeMapping(): Unit = {
        LinkitApplication.mapEnvironment(LocalFileSystemAdapters.Nio, Seq(getClass))
    }

    @Test
    def simplePacketTest(): Unit = {
        val obj = new util.HashMap[Int, String]() {
            put(0, "GENS")
            put(-1, "LES")
            put(-2, "SALUT")
        }
        testPacket(obj)
    }

    @Test
    def moreComplexPacketTest(): Unit = {
        val obj = Array(AnyRefPacket(Some(CacheRepoContent(Array(PuppetProfile(Array[Int](1, 3, 5), ListBuffer(), "TestServer1"))))))
        testPacket(obj)
    }

    @Test
    def complexPacketTest(): Unit = {
        val packet = ArrayBuffer(DedicatedPacketCoordinates(12, "TestServer1", "s1"), SimplePacketAttributes("family" -> "Global Cache", "behavior" -> CacheSearchBehavior.GET_OR_OPEN), RequestPacket(1, Array(IntPacket(3))))
        testPacket(packet)
    }

}

object PacketTests {

    private val serializer = new DefaultSerializer

    def testPacket(obj: AnyRef): Unit = {
        val packet = RefPacket(obj)
        println(s"Serializing packet $packet...")
        val bytes = serializer.serialize(packet, true)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (size: ${bytes.length})")
        val packet2 = serializer.deserialize(bytes)
        println(s"deserialized packet = ${packet2}")
    }

}
