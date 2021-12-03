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

package fr.linkit.engine.internal.concurrency

import fr.linkit.api.internal.concurrency.WorkerPools.{currentTask, currentWorker}
import fr.linkit.api.internal.concurrency._
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.internal.utils.ConsumerContainer
import org.jetbrains.annotations.Nullable

import java.util.concurrent.locks.LockSupport
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class SimpleAsyncTask[A](override val taskID: Int, @Nullable override val parent: AsyncTask[_] with AsyncTaskController, task: () => Try[A]) extends AsyncTask[A] with AsyncTaskController {

    @volatile private var attempt: Try[A]       = _
    @volatile private var paused                = true
    private val onCompleteConsumers             = new ConsumerContainer[Try[A]]
    private val onThrowConsumers                = new ConsumerContainer[Option[Throwable]]
    private           var worker : Worker = _

    override def getWorkerThread: Worker = worker

    override def isExecuting: Boolean = worker != null

    override def join(): Try[A] = {
        awaitComplete(LockSupport.park(this), LockSupport.unpark).get
    }

    override def join(millis: Long): Option[Try[A]] = {
        if (millis <= 0) {
            return Option(join())
        }
        awaitComplete(LockSupport.parkNanos(this, millis * 1000000), t => {
            if (LockSupport.getBlocker(t) eq this)
                LockSupport.unpark(t)
        })
    }

    @workerExecution
    override def derivate(): Try[A] = {
        val pool       = WorkerPools.ensureCurrentIsWorker()
        val pausedTask = currentTask.get
        awaitComplete(pool.pauseCurrentTask(), _ => {
            pausedTask.wakeup()
        }).get
    }

    @workerExecution
    override def derivateForAtLeast(millis: Long): Option[Try[A]] = {
        val pool = WorkerPools.ensureCurrentIsWorker()
        val task = currentTask
        awaitComplete(pool.pauseCurrentTaskForAtLeast(millis), _ => task.get.wakeup())
    }

    @workerExecution
    def runTask(): Unit = {
        WorkerPools.ensureCurrentIsWorker()

        paused = false
        worker = currentWorker
        this.attempt = task.apply()
        //AppLogger.info(s"$this -> Task attempt: $attempt")

        onCompleteConsumers.synchronized {
            onCompleteConsumers.applyAll(attempt)
        }
        val opt: Option[Throwable] = attempt match {
            case Failure(exception) =>
                notifyNestThrow(exception)
                Option(exception)
            case Success(_)         => None
        }
        onThrowConsumers.applyAll(opt)
        worker = null
    }

    override def notifyNestThrow(threw: Throwable): Unit = {
        //AppLogger.debug(s"NotifyNestThrow : $threw")
        //AppLogger.debug(s"onThrowConsumers: $onThrowConsumers")
        //AppLogger.debug(s"this            : $this")

        val consumersEmpty = onThrowConsumers.isEmpty
        onThrowConsumers.applyAll(Option(threw))
        if (parent != null && consumersEmpty)
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
        val future = SimpleAsyncTask[S](taskID, this)(Try(f(attempt)).flatten)
        onComplete(_ => future.runTask())
        future
    }

    override def transformWith[S](f: Try[A] => Future[S])(implicit executor: ExecutionContext): Future[S] = {
       throw new UnsupportedOperationException("Method signature not understood.") //TODO "Method signature not understood."
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
        else onCompleteConsumers.synchronized(onCompleteConsumers +:+= (t => f(t)))
    }

    private def awaitComplete(makeWait: => Unit, onWakeup: Thread => Unit): Option[Try[A]] = {
        if (isCompleted)
            return Option(attempt)

        val parkedThread = currentThread
        onCompleteConsumers.synchronized {
            onCompleteConsumers +:+= (_ => {
                onWakeup(parkedThread)
            })
        }
        makeWait
        setContinue()
        Option(attempt)
    }

    override def throwNextThrowable(): Unit = {
        if (isCompleted) {
            return
        }
        val (waitAction, wakeupAction) = if (WorkerPools.isCurrentThreadWorker) {
            val pool = WorkerPools.currentPool.get
            val task = currentTask
            (() => pool.pauseCurrentTask(), () => {
                task.get.wakeup()
            })
        } else {
            val thread = currentThread
            (() => LockSupport.park(this), () => LockSupport.unpark(thread))
        }

        @volatile var exception: Option[Throwable] = None
        addOnNextThrow(t => {
            exception = t
            wakeupAction.apply()
        })
        waitAction.apply()
        setContinue()
        if (exception.isDefined) {
            throw exception.get
        }
    }

    override def addOnNextThrow(callback: Option[Throwable] => Unit): Unit = {
        onThrowConsumers +:+= callback
    }

    override def isPaused: Boolean = paused

    override def wakeup(): Unit = {
        setContinue()
        if (worker != null)
            worker.getController.wakeup(this)
    }

    override def setPaused(): Unit = {
        //AppLogger.vInfo(s"TASK $this MARKED AS PAUSED")
        paused = true
    }

    override def setContinue(): Unit = {
        //AppLogger.vInfo(s"TASK $this MARKED AS CONTINUE")
        paused = false
    }

    override def toString: String = s"SimpleAsyncTask($taskID, $parent)"

}

object SimpleAsyncTask {

    def apply[A](taskID: Int, parent: AsyncTask[_] with AsyncTaskController)(task: => Try[A]): SimpleAsyncTask[A] = new SimpleAsyncTask(taskID, parent, () => task)

}
