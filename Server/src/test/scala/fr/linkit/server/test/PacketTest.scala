/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.test

import fr.linkit.api.gnom.cache.CacheSearchBehavior
import fr.linkit.api.gnom.packet.DedicatedPacketCoordinates
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import fr.linkit.engine.internal.utils.ScalaUtils
import fr.linkit.server.test.PacketTest.{connection, serialAndDeserial}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}

@TestInstance(Lifecycle.PER_CLASS)
class PacketTest {
    PacketTest // Load statics

    @Test
    def serialAndDeserialObject(): Unit = {
        val gc      = connection.network.globalCache
        val content = gc.retrieveCacheContent(0, CacheSearchBehavior.GET_OR_CRASH)
        for (_ <- 0 to 100) {
            serialAndDeserial(content)
        }
    }


}

object PacketTest {

    private val app        = ServerLauncher.launch()
    private val connection = app.findConnection("TestServer1").get
    private val translator = connection.translator

    private val coords     = DedicatedPacketCoordinates(Array(0, 1), "1", "TestServer1")
    private val attributes = SimplePacketAttributes.empty
    private val traffic    = connection.traffic
    private val config     = traffic.defaultPersistenceConfig
    private val gnol       = connection.network.gnol

    def serialAndDeserial(obj: AnyRef): Unit = {
        println(s"Serializing and deserializing object $obj")
        val serialResult = translator.translate(SimpleTransferInfo(coords, attributes, ObjectPacket(obj), config, gnol))
        val buff         = serialResult.buff
        println("Packet bytes: " + ScalaUtils.toPresentableString(buff) + s" (size: ${buff.limit()} bytes)")
        buff.position(4)
        val deserialResult = translator.translate(traffic, buff)
        deserialResult.makeDeserialization()
        val result = deserialResult.packet.asInstanceOf[ObjectPacket].value
        println(s"Deserialization result: $result")
    }

}