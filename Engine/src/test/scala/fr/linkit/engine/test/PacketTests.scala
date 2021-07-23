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

import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.persistence.DefaultSerializer
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.utils.ScalaUtils
import fr.linkit.engine.test.classes.Player
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeAll, Test, TestInstance}

import scala.collection.mutable.ListBuffer

@TestInstance(Lifecycle.PER_CLASS)
class PacketTests {

    private val serializer = new DefaultSerializer

    @BeforeAll
    def makeMapping(): Unit = {
        LinkitApplication.mapEnvironment(LocalFileSystemAdapters.Nio, Seq(getClass))
    }

    @Test
    def simplePacketTest(): Unit = {
        val player = Player(0, "test", "test", 0, 0)
        testPacket(player)
    }

    @Test
    def moreComplexPacketTest(): Unit = {
        val player = Player(0, "test", "test", 0, 0)
        testPacket(Array("jammy" -> player, (player, player)))
    }

    private def testPacket(obj: AnyRef): Unit = {
        val packet = RefPacket(obj)
        println(s"Serializing packet $packet...")
        val bytes = serializer.serialize(packet, true)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (size: ${bytes.length})")
        val packet2 = serializer.deserialize(bytes)
        println(s"deserialized packet = ${packet2}")
    }

}
