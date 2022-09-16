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

package fr.linkit.api.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{AsyncTask, AsyncTaskController, IllegalThreadException, Worker, workerExecution}
import fr.linkit.api.internal.system.delegate.DelegateFactory

import scala.concurrent.ExecutionContext

object WorkerPools {
    private val delegate = DelegateFactory.workerPools

    def newHiringPool(name: String): HiringWorkerPool = delegate.newHiringPool(name)

    def currentTaskWithController: Option[AsyncTask[_] with AsyncTaskController] = delegate.currentTaskWithController

    def currentWorker: Worker = delegate.currentWorker

    def currentWorkerOpt: Option[Worker] = delegate.currentWorkerOpt

    implicit def currentExecutionContext: ExecutionContext = delegate.currentExecutionContext

    def currentTask: Option[AsyncTask[_]] = delegate.currentTask

    /**
     * @return Some if the current thread is a member of a [[WorkerPool]], None instead
     * */
    implicit def currentPool: Option[WorkerPool] = delegate.currentPool

    /**
     * Toggles between two actions if the current thread is an instance of [[Worker]]
     *
     * @param ifCurrent The action to process if the current thread is a worker thread.
     *                  The given entry is the current thread pool
     * @param orElse    the action to process if the current thread is not a worker thread.
     *
     * */
    def ifCurrentWorkerOrElse[A](ifCurrent: WorkerPool => A, orElse: => A): A = delegate.ifCurrentWorkerOrElse(ifCurrent, orElse)

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[Worker]]
     * */
    def isCurrentThreadWorker: Boolean = delegate.isCurrentThreadWorker

    /**
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * @param msg the message to complain with the exception
     * */
    def ensureCurrentIsNotWorker(msg: String): Unit = delegate.ensureCurrentIsNotWorker(msg)

    /**
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * */
    def ensureCurrentIsNotWorker(): Unit = delegate.ensureCurrentIsNotWorker()

    /**
     * @throws IllegalThreadException if the current thread is a [[Worker]]
     * @param msg the message to complain with the exception
     * */
    def ensureCurrentIsWorker(msg: String): WorkerPool = delegate.ensureCurrentIsWorker(msg)

    /**
     * @throws IllegalThreadException if the current thread is a [[Worker]]
     * */
    def ensureCurrentIsWorker(): WorkerPool = delegate.ensureCurrentIsWorker()

    def runLaterInCurrentPool(action: => Unit): Unit = delegate.runLaterInCurrentPool(action)

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread is not a worker thread, this would mean that
     * we are not running into a thread that is owned by the concurrency system. Therefore, the action
     * may be performed in the current thread
     *
     * @param action the action to perform
     * */
    def runLaterOrHere(action: => Unit): Unit = delegate.runLaterOrHere(action)

    def newClosedPool(name: String, initialThreadCount: Int = 0): ClosedWorkerPool = delegate.newClosedPool(name, initialThreadCount)

    private[linkit] trait Provider {

        def newHiringPool(name: String): HiringWorkerPool

        @workerExecution
        def currentTaskWithController: Option[AsyncTask[_] with AsyncTaskController]

        @workerExecution
        def currentWorker: Worker

        def currentWorkerOpt: Option[Worker]

        implicit def currentExecutionContext: ExecutionContext

        @workerExecution
        def currentTask: Option[AsyncTask[_]]

        implicit def currentPool: Option[WorkerPool]

        def ifCurrentWorkerOrElse[A](ifCurrent: WorkerPool => A, orElse: => A): A

        def isCurrentThreadWorker: Boolean

        def ensureCurrentIsNotWorker(msg: String): Unit

        def ensureCurrentIsNotWorker(): Unit

        def ensureCurrentIsWorker(msg: String): WorkerPool

        def ensureCurrentIsWorker(): WorkerPool

        def runLaterInCurrentPool(@workerExecution action: => Unit): Unit

        def runLaterOrHere(action: => Unit): Unit

        def newClosedPool(name: String, initialThreadCount: Int = 0): ClosedWorkerPool
    }
}
