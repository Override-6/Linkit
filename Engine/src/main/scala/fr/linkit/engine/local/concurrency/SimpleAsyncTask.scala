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

import fr.linkit.api.local.concurrency.{AsyncTask, AsyncTaskController, WorkerThread, workerExecution}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool.currentTask
import fr.linkit.engine.local.utils.ConsumerContainer
import org.jetbrains.annotations.Nullable

import java.util.concurrent.locks.LockSupport
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class SimpleAsyncTask[A](override val taskID: Int, @Nullable override val parent: AsyncTask[_] with AsyncTaskController, task: () => Try[A]) extends AsyncTask[A] with AsyncTaskController {

    @volatile private var attempt: Try[A]                 = _
    @volatile private var paused                          = true
    private val onCompleteConsumers                       = new ConsumerContainer[Try[A]]
    private val onThrowConsumers                          = new ConsumerContainer[Option[Throwable]]
    private           var worker : ControlledWorkerThread = _

    override def getWorkerThread: WorkerThread = worker

    override def isExecuting: Boolean = worker != null

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
        val pool = BusyWorkerPool.ensureCurrentIsWorker()

        val task = currentTask
        awaitComplete(pool.pauseCurrentTask(), _ => task.wakeup()).get
    }

    @workerExecution
    override def joinTaskForAtLeast(millis: Long): Option[Try[A]] = {
        val pool = BusyWorkerPool.ensureCurrentIsWorker()
        val task = currentTask
        awaitComplete(pool.pauseCurrentTaskForAtLeast(millis), _ => task.wakeup())
    }

    @workerExecution
    def runTask(): Unit = {
        BusyWorkerPool.ensureCurrentIsWorker()

        paused = false
        this.attempt = task.apply()

        onCompleteConsumers.applyAll(attempt)
        val opt: Option[Throwable] = attempt match {
            case Failure(exception) => Option(exception)
            case Success(_)         => None
        }
        onThrowConsumers.applyAll(opt)
        worker = null
    }

    override def notifyNestThrow(threw: Throwable): Unit = {
        onThrowConsumers.applyAll(Option(threw))
        if (parent != null)
            parent.notifyNestThrow(threw)
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
        val future = SimpleAsyncTask[S](taskID, this, Try(f(attempt)).flatten)
        onComplete(_ => future.runTask())
        future
    }

    override def transformWith[S](f: Try[A] => Future[S])(implicit executor: ExecutionContext): Future[S] = {
        /*val future = SimpleAsyncTaskFuture[S](taskID, Try(f(attempt)))
        onComplete(attempt => future.runTask())
        future*/
        throw new UnsupportedOperationException("Method signature not understood.")
    }

    override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
        atMost match {
            case _: Duration.Infinite     => join()
            case duration: FiniteDuration => join(duration.toMillis)
        }
        this
    }

    override def result(atMost: Duration)(implicit permit: CanAwait): A = {
        ready(atMost)
        attempt.get
    }

    private def performOnComplete[U](f: Try[A] => U): Unit = {
        if (isCompleted)
            f(attempt)
        else onCompleteConsumers += (t => f(t))
    }

    private def awaitComplete(wait: => Unit, wakeup: Thread => Unit): Option[Try[A]] = {
        if (isCompleted)
            return Option(attempt)

        val parkedThread = currentThread
        onCompleteConsumers += (e => wakeup(parkedThread))
        wait
        Option(attempt)
    }

    override def awaitNextThrowable(): Unit = {
        if (isCompleted)
            return
        val (waitAction, wakeupAction) = if (BusyWorkerPool.isCurrentThreadWorker) {
            val pool = BusyWorkerPool.currentPool.get
            val task = currentTask
            (() => pool.pauseCurrentTask(), () => task.wakeup())
        } else {
            val thread = currentThread
            (() => LockSupport.park(), () => LockSupport.unpark(thread))
        }

        var exception: Throwable = null
        addOnNextThrow(t => {
            exception = t.orNull
            wakeupAction.apply()
        })
        waitAction.apply()
        if (exception != null)
            throw exception
    }

    override def addOnNextThrow(callback: Option[Throwable] => Unit): Unit = {
        onThrowConsumers += callback
    }

    override def isPaused: Boolean = paused

    override def wakeup(): Unit = {
        setContinue()
    }

    override def setPaused(): Unit = paused = true

    override def setContinue(): Unit = paused = false

    override def isWaitingToContinue: Boolean = !paused && isExecuting
}

object SimpleAsyncTask {

    def apply[A](taskID: Int, parent: AsyncTask[_] with AsyncTaskController, task: => Try[A]): SimpleAsyncTask[A] = new SimpleAsyncTask(taskID, parent, () => task)

}
