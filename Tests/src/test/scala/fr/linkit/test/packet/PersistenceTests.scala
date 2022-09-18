/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.test.packet

import fr.linkit.api.gnom.packet.Packet
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.StringPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.SyncPacketChannel
import fr.linkit.test.TestEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._

import java.util.concurrent.ThreadLocalRandom
import scala.reflect.ClassTag

class PersistenceTests {
    
    private final val clientChannel = TestEngine.clientSideNetwork.connection.traffic.getInjectable(201, SyncPacketChannel, ChannelScopes.broadcast)
    private final val serverChannel = TestEngine.serverSideNetwork.connection.traffic.getInjectable(201, SyncPacketChannel, ChannelScopes.broadcast)
    
    private def testPacket[P <: Packet : ClassTag](packet: P, print: Boolean = true)(assertions: ((P, P) => Unit)*): Unit = {
        if (print)
            println(s"Serializing packet '$packet'...")
        clientChannel.send(packet)
        if (print)
            println(s"Deserializing packet...")
        val result = serverChannel.nextPacket[P]
        for (assertion <- assertions)
            assertion(packet, result)
    }
    
    @Test
    def testSimplePacket(): Unit = {
        testPacket(StringPacket("test"))(assertEquals)
    }
    
    @Test
    def testWeirdStringPacket(): Unit = {
        val bytes = new Array[Byte](2000)
        ThreadLocalRandom.current().nextBytes(bytes)
        
        testPacket(StringPacket(new String(bytes)), false)(assertEquals)
    }
    
}

object PersistenceTests {
}
