/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.test

import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.api.test.TestUtils._
import fr.linkit.engine.internal.concurrency.pool.BusyWorkerPool
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._
import org.junit.jupiter.api.function.Executable

@TestInstance(Lifecycle.PER_CLASS)
class ConcurrencyTests {

    private val pool = new BusyWorkerPool(2, "TestPool")

    @Test
    @RepeatedTest(5)
    def join(): Unit = runLater {
        assertBetween(2000, 2100, {
            pool.runLaterControl {
                AppLogger.debug(s"${currentTasksId} Waiting 1s...")
                Thread.sleep(2000L)
                AppLogger.debug(s"${currentTasksId} Freedom")
            }.join()
            AppLogger.debug("Hello")
        }: Executable)
    }

    @RepeatedTest(5)
    def join1s(): Unit = runLater {
        assertBetween(1000, 1200, {
            val task = pool.runLaterControl {
                AppLogger.debug("Waiting 2s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }
            AppLogger.debug(s"current task ${WorkerPools.currentTask.taskID} is about to join task ${task.taskID} for 1 second...")
            task.join(1000)
            AppLogger.debug("join ended.")
        }: Executable)
    }

    @RepeatedTest(5)
    def throwNextThrowable(): Unit = {
        runLater {
            AppLogger.debug("setting AssertThrows...")
            Assertions.assertThrows(classOf[ArithmeticException], try {
                pool.runLaterControl {
                    pool.runLaterControl {
                        pool.runLaterControl {
                            AppLogger.debug("throwing...")
                            throw new ArithmeticException(s"${System.nanoTime()}")
                        }
                        pool.pauseCurrentTaskForAtLeast(1000)
                    }
                    pool.pauseCurrentTaskForAtLeast(1000)
                }.throwNextThrowable()
            }: Executable catch {
                case e: Throwable =>
                    e.printStackTrace()
                    throw e
            })
            AppLogger.debug("AssertThrows set !")
        }
    }

    @RepeatedTest(5)
    def derivate(): Unit = runLater {
        assertBetween(2000, 2200, {
            pool.runLaterControl {
                AppLogger.debug("Waiting 2s...")
                Thread.sleep(2000L)
                AppLogger.debug("Freedom")
            }.derivate()
            AppLogger.debug("Hello")
        }: Executable)
    }

    @RepeatedTest(5)
    def derivateForAtLeast1s(): Unit = runLater {
        assertBetween(1000, 1200000, {
            pool.runLaterControl {
                pool.runLaterControl {
                    AppLogger.debug("Waiting 1s...")
                    Thread.sleep(2000L)
                    AppLogger.debug("Freedom")
                }.derivate()
                AppLogger.debug("Hello")
            }.derivateForAtLeast(1000L)
            AppLogger.debug("Hello")
        }: Executable)
    }

    @AfterAll
    def closePool(): Unit = pool.close()

    private def runLater(f: => Unit): Unit = {
        pool
                .runLaterControl(f)
                .throwNextThrowable()
    }

    private def assertBetween(min: Int, max: Int, action: => Unit): Unit = {
        val t0 = System.currentTimeMillis()
        AppLogger.trace(s"AssertBetween $min ms and $max ms...")
        action
        val t1       = System.currentTimeMillis()
        val execTime = t1 - t0
        AppLogger.trace(s"Action performed in $execTime ms")
        if (execTime < min || execTime > max)
            throw new AssertionError(s"execution did not took between $min ms and $max ms to execute. ($execTime ms)")
    }

}
