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

import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.test.TestUtils._
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.{AfterAll, Assertions, Test, TestInstance}

import java.time.Duration

@TestInstance(Lifecycle.PER_CLASS)
class ConcurrencyTests {

    private val pool = new BusyWorkerPool(2, "TestPool")

    @Test
    def join(): Unit = runLater {
        Assertions.assertTimeout(Duration.ofSeconds(2), {
            pool.runLaterControl {
                AppLogger.debug("Waiting 1s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }.join()
        }: Executable)
    }

    @Test
    def join1s(): Unit = runLater {
        Assertions.assertTimeout(Duration.ofMillis(1100), {
            pool.runLaterControl {
                AppLogger.debug("Waiting 1s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }.join(1000)
        }: Executable)
    }

    @Test
    def awaitFirstThrowable(): Unit = {
        println("dqsdqzd")
        runLater {
            println(s"Thread.currentThread() = ${Thread.currentThread()}")
            Assertions.assertThrows(classOf[ArithmeticException], {
                pool.runLaterControl {
                    println(s"Thread.currentThread() = ${Thread.currentThread()}")
                    pool.runLaterControl {
                        println(s"Thread.currentThread() = ${Thread.currentThread()}")
                        pool.runLaterControl {
                            println(s"Thread.currentThread() = ${Thread.currentThread()}")
                            throw new ArithmeticException()
                        }
                        pool.pauseCurrentTaskForAtLeast(1000)
                    }
                    pool.pauseCurrentTaskForAtLeast(1000)
                }//.awaitNextThrowable()
            }: Executable)
        }
    }

    @Test
    def joinTask(): Unit = runLater {
        Assertions.assertTimeout(Duration.ofSeconds(1), {
            pool.runLaterControl {
                AppLogger.debug("Waiting 2s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }.derivate()
        }: Executable)
    }

    @Test
    def joinTaskForAtLeast1s(): Unit = runLater {
        Assertions.assertTimeout(Duration.ofSeconds(1), {
            pool.runLaterControl {
                AppLogger.debug("Waiting 1s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }.derivateForAtLeast(1000L)
        }: Executable)
    }

    @AfterAll
    def closePool(): Unit = pool.close()

    private def runLater(f: => Unit): Unit = pool
        .runLaterControl(f)
        .throwNextThrowable()

}
