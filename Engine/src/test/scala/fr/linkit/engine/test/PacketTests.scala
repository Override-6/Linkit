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

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

@TestInstance(Lifecycle.PER_CLASS)
class PacketTests {

    @BeforeAll
    def makeMapping(): Unit = {
        LinkitApplication.mapEnvironment(LocalFileSystemAdapters.Nio, Seq(getClass))
    }

    @Test
    def simplePacketTest(): Unit = {
        val obj = mutable.HashSet("SALUT", "LES", "GENS")
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
        val perf1  = new PerformanceMeter
        val perf2  = new PerformanceMeter
        for (i <- 0 to 100) {
            testPacket(packet)
            perf2.printPerf("Serial and deserial")
        }
        perf1.printPerf("Total")
    }

    @Test
    def specificDeserialisation(): Unit = {
        val array: Array[Byte] = Array(1, 0, 1, -1, -119, 109, 52, -35, 27, -127, 1, 2, 1, 27, 1, 63, -116, -57, 15, 64, -75, 1, 3, 1, 12, -126, 1, 5, -120, 84, 101, 115, 116, 83, 101, 114, 118, 101, 114, 49, -120, 115, 49, -116, -21, -25, -25, 76, -116, 18, 0, -80, -34, -119, -103, -98, 31, 50, -127, 1, 1, 1, 16, -116, -103, -98, 31, 50, 1, 6, -120, 99, 97, 99, 104, 101, -126, 1, 50, -116, -103, -98, 31, 50, 1, 7, -120, 102, 97, 109, 105, 108, 121, -120, 71, 108, 111, 98, 97, 108, 32, 67, 97, 99, 104, 101, -116, -86, -47, 73, 33, 1, 17, 1, 3, -116, -21, -25, -25, 76, -116, 18, 0, -80, -34, -119, -103, -98, 31, 50, -127, -105, -126, 1, 13, -119, -127, 77, -47, -103, -127, 1, 0, -116, 70, 36, -16, 95, 1, 1, 1, 6, 1, 17, -117, -126, 4, 109, -71, 1, -78, -119, 63, 105, 121, -109, -127, 1, 0, -120, 77, 105, 99, 104, 101, 108, 108, 101, -119, 0, 1, -105, -17, -127, 1, 1, 1, 3, -126, 1, 0, -126, 4, 114, 34, -60, 13)
        val packet = new DefaultSerializer().deserialize(array)
        println(s"packet = ${packet}")
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
