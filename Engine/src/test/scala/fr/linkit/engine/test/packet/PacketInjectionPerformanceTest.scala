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

package fr.linkit.engine.test.packet

import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.engine.connection.packet.traffic.{ChannelScopes, SocketPacketTraffic}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.utils.PerformanceMeter
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

@TestInstance(Lifecycle.PER_CLASS)
class PacketInjectionPerformanceTest {

    private val workerPool  = new BusyWorkerPool(4, "TestPool")
    private val traffic     = new SocketPacketTraffic(null, null, workerPool, "test", "test")
    private val testChannel = traffic.createStore(0)
            .createStore(1)
            .createStore(2)
            .getInjectable(3, SyncAsyncPacketChannel, ChannelScopes.include("test"))

    @Test
    def testOnePacket(): Unit = {
        testNPacket(1)
    }

    @Test
    def testTenPacket(): Unit = {
        testNPacket(10)
    }

    @Test
    def testHundredPacket(): Unit = {
        testNPacket(100)
    }

    @Test
    def testThousandPackets(): Unit = {
        testNPacket(1000)
    }

    private def testNPacket(n: Int): Unit = {
        println(s"Path is ${testChannel.path.mkString("/")}.")
        val meter = new PerformanceMeter()
        workerPool.runLater {
            try {
                var i     = 0
                while (i <= n) {
                    val number = testChannel.nextSync[IntPacket].value
                    println(s"Number : $number, ${Thread.currentThread()}")
                    Assertions.assertEquals(i, number)
                    i += 1
                }
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }
        for (x <- 0 to n) workerPool.runLater {
            val millis = System.currentTimeMillis()
            println(s"$millis : Send Sync ${x}, ${Thread.currentThread()}")
            testChannel.sendSync(IntPacket(x))
        }
        meter.printPerf("Total")
        println("Injection done.")
    }

}
