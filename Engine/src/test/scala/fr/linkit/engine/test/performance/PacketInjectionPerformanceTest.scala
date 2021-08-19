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

package fr.linkit.engine.test.performance

import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.engine.connection.packet.traffic.{ChannelScopes, SocketPacketTraffic}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.utils.PerformanceMeter
import org.junit.jupiter.api.{Assertions, Test}

class PacketInjectionPerformanceTest {

    @Test
    def testThousandPackets(): Unit = {
        val workerPool  = new BusyWorkerPool(2, "TestPool")
        val traffic     = new SocketPacketTraffic(null, null, workerPool, "test", "test")
        val testChannel = traffic.createStore(0)
                .createStore(1)
                .createStore(2)
                .getInjectable(3, SyncAsyncPacketChannel, ChannelScopes.include("test"))
        println(s"Path is ${testChannel.path.mkString("/")}.")
        val meter = new PerformanceMeter()
        var i = 0
        testChannel.addAsyncListener(bundle => {
            val packet = bundle.packet
            val number = packet.asInstanceOf[IntPacket].value
            //println(s"Number : $number")
            Assertions.assertEquals(i, number)
            i += 1
        })
        for (i <- 0 to 1000) workerPool.runLater {
            testChannel.sendAsync(IntPacket(i))
        }
        meter.printPerf("Total")
        println("Injection done.")
    }

}
