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

package fr.linkit.api.internal.concurrency

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.util.Try

object WorkerPools {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Application Worker")

    private val boundedThreads = mutable.Map.empty[Thread, Worker]

    def bindWorker(thread: Thread, workerVersion: Worker): Unit = {
        boundedThreads.put(thread, workerVersion)
    }

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread is not a worker thread, this would mean that
     * we are not running into a thread that is owned by the concurrency system. Therefore, the action
     * may be performed in the current thread
     *
     * @param action the action to perform
     * */
    def runLaterOrHere(action: => Unit): Unit = {
        currentPool.fold(action)(_.runLater(action))
    }

    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def runLaterInCurrentPool(@workerExecution action: => Unit): Unit = {
        val pool = ensureCurrentIsWorker("Could not run request action because current thread does not belong to any worker pool")
        pool.runLater(action)
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[Worker]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(): WorkerPool = {
        if (!isCurrentThreadWorker)
            throw IllegalThreadException("This action must be performed by a Worker thread !")
        currentPool.get
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[Worker]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(msg: String): WorkerPool = {
        if (!isCurrentThreadWorker)
            throw IllegalThreadException(s"This action must be performed by a Worker thread ! ($msg)")
        currentPool.get
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def ensureCurrentIsNotWorker(): Unit = {
        if (isCurrentThreadWorker)
            throw IllegalThreadException("This action must not be performed by a Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def ensureCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentThreadWorker)
            throw IllegalThreadException(s"This action must not be performed by a Worker thread ! ($msg)")
    }

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[Worker]]
     * */
    def isCurrentThreadWorker: Boolean = {
        Try(currentWorker).isSuccess
    }

    /**
     * Toggles between two actions if the current thread is an instance of [[Worker]]
     *
     * @param ifCurrent The action to process if the current thread is a worker thread.
     *                  The given entry is the current thread pool
     * @param orElse    the action to process if the current thread is not a worker thread.
     *
     * */
    def ifCurrentWorkerOrElse[A](ifCurrent: WorkerPool => A, orElse: => A): A = {
        val pool = currentPool
        if (pool.isDefined) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
    }

    /**
     * @return Some if the current thread is a member of a [[WorkerPool]], None instead
     * */
    implicit def currentPool: Option[WorkerPool] = {
        findCurrentWorker.map(_.pool)
    }

    @workerExecution
    def currentTask: Option[AsyncTask[_]] = {
        findCurrentWorker.flatMap(_.getCurrentTask)
    }

    implicit def currentExecutionContext: ExecutionContext = {
        currentPool match {
            case Some(pool) => pool
            case None       => global
        }
    }

    def findCurrentWorker: Option[Worker] = {
        currentThread match {
            case worker: Worker => Some(worker)
            case other          => boundedThreads.get(other)
        }
    }

    @workerExecution
    def currentWorker: Worker = {
        findCurrentWorker.getOrElse(throw IllegalThreadException(s"Current thread is not a WorkerThread. ($currentThread)"))
    }

    @workerExecution
    def currentTaskWithController: Option[AsyncTask[_] with AsyncTaskController] = {
        if (!isCurrentThreadWorker)
            return None
        currentWorker.getController.getCurrentTask
    }

    def currentTasksId: String = {
        if (isCurrentThreadWorker)
            currentWorker.prettyPrintPrefix
        else "?"
    }

    private def currentThread: Thread = Thread.currentThread()

}
