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

package fr.linkit.engine.local.concurrency

import fr.linkit.api.local.concurrency.{AsyncTaskFuture, workerExecution}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool.currentWorker
import fr.linkit.engine.local.concurrency.pool.{BusyWorkerPool, BusyWorkerThread}
import fr.linkit.engine.local.utils.ConsumerContainer

import java.util.concurrent.locks.LockSupport
import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class SimpleAsyncTaskFuture[A](override val taskID: Int, task: () => Try[A]) extends AsyncTaskFuture[A] {

    @volatile private var attempt: Try[A] = _
    private val consumers                 = new ConsumerContainer[Try[A]]

    @workerExecution
    def runTask(): Unit = {
        BusyWorkerPool.ensureCurrentIsWorker()

        val attempt = task.apply()
        this.attempt = attempt
        consumers.applyAll(attempt)
    }

    override def join(): Try[A] = {
        awaitComplete(LockSupport.park(), LockSupport.unpark).get
    }

    override def join(millis: Long): Option[Try[A]] = {
        if (millis <= 0) {
            join()
            return Option(attempt)
        }
        awaitComplete(LockSupport.parkNanos(millis * 1000000), LockSupport.unpark)
    }

    @workerExecution
    override def joinTask(): Try[A] = {
        val pool   = BusyWorkerPool.ensureCurrentIsWorker()
        val worker = currentWorker
        val taskID = worker.currentTaskID
        awaitComplete(pool.pauseCurrentTask(), t => BusyWorkerPool.unpauseTask(worker, taskID)).get
    }

    @workerExecution
    override def joinTaskForAtLeast(millis: Long): Option[Try[A]] = {
        val pool   = BusyWorkerPool.ensureCurrentIsWorker()
        val worker = currentWorker
        val taskID = worker.currentTaskID
        awaitComplete(pool.pauseCurrentTaskForAtLeast(millis), t => BusyWorkerPool.unpauseTask(worker, taskID))
    }

    override def onComplete[U](f: Try[A] => U)(implicit executor: ExecutionContext): Unit = {
        performOnComplete(attempt => executor.execute { () =>
            try {
                f(attempt)
            } catch {
                case NonFatal(e) => executor.reportFailure(e)
            }
        })
    }

    override def isCompleted: Boolean = attempt != null

    override def value: Option[Try[A]] = if (!isCompleted) None else Some(attempt)

    override def transform[S](f: Try[A] => Try[S])(implicit executor: ExecutionContext): Future[S] = {
        val future = SimpleAsyncTaskFuture[S](taskID, Try(f(attempt)).flatten)
        onComplete(attempt => future.runTask())
        future
    }

    override def transformWith[S](f: Try[A] => Future[S])(implicit executor: ExecutionContext): Future[S] = {
        /*val future = SimpleAsyncTaskFuture[S](taskID, Try(f(attempt)))
        onComplete(attempt => future.runTask())
        future*/
        throw new UnsupportedOperationException("Method signature not understood.")
    }

    override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
        join(atMost.toMillis)
        this
    }

    override def result(atMost: Duration)(implicit permit: CanAwait): A = ???

    private def performOnComplete[U](f: Try[A] => U): Unit = {
        if (isCompleted)
            f(attempt)
        else consumers += (t => f(t))
    }

    private def awaitComplete(wait: => Unit, wakeup: Thread => Unit): Option[Try[A]] = {
        if (isCompleted)
            return Option(attempt)

        val parkedThread = currentThread
        consumers += (e => wakeup(parkedThread))
        wait
        Option(attempt)
    }

}

object SimpleAsyncTaskFuture {

    def apply[A](taskID: Int, task: => Try[A]): SimpleAsyncTaskFuture[A] = new SimpleAsyncTaskFuture(taskID, () => task)

}
