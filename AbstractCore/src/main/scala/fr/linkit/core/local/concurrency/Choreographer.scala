/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.local.concurrency

import scala.collection.mutable.ListBuffer

//TODO
class Choreographer(pool: BusyWorkerPool, maxThreadCount: Int, threadChunkSize: Int, threadMargin: Int) {

    type WorkerThread = BusyWorkerPool#WorkerThread

    private val threads  = ListBuffer.empty[WorkerThread]
    private val poolName = pool.name

    def notifyTaskSubmit(): Unit = {
        val threadCount                    = threads.length
        val (countRunning, countDisturbed) = countRunningThreads
        val activeThreads                  = countRunning + countDisturbed

        val minCount = threadCount - threadMargin
        val maxCount = threadCount + threadMargin
        if (maxCount > activeThreads && activeThreads > minCount)
            return //Pool running count is between min and max count : Thread pool is equilibrated.

        if (activeThreads == 0 && threadCount >= maxThreadCount) {
            //TODO
            throw new SuffocatingPoolException(
                """
                  |All thread of pool '$poolName' are terminated or waiting.
                  |The maximum thread count has been reached and the Choreographer have no choice to throw an exception because
                  |The current application could be deadlocked.
                  |(You can choose '--cgps-deadlock-[prone|warn|pass]' in order to change system choreographers behaviour over deadlocks.
                  |""".stripMargin
            )
        }
        //if ()
    }

    def notifyTaskTaken(): Unit = {

    }

    def addThread(thread: WorkerThread): Unit = {
        /*if (thread.ownerPool != pool)
            throw new IllegalArgumentException(s"Given unexpected thread to ${poolName}'s Choreographer. (pools mismatches !)")

        threads += thread*/
    }

    private def countRunningThreads: (Int, Int) = {
        var running   = 0
        var disturbed = 0
        for (worker <- threads) {
            val state = worker.getState
            import Thread.State._
            state match {
                case NEW => running += 1 //The thread will soon be ready to run.
                case WAITING => //if (worker.inBusyLock) running += 1 //Thread considered as runnable if the thread is waiting for any task to be submit.
                case TIMED_WAITING | BLOCKED => disturbed += 1
                case TERMINATED => //Not counted.
            }
        }
        (running, disturbed)
    }

}
