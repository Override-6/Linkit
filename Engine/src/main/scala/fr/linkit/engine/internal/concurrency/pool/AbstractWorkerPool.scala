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

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.gnom.network.ExecutorEngine
import fr.linkit.api.internal.concurrency.pool.WorkerPool
import fr.linkit.api.internal.concurrency.{IllegalThreadException, Worker, WorkerTask, workerExecution}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.concurrency.pool.EngineWorkerPools._
import fr.linkit.engine.internal.concurrency.{SimpleAsyncTask, now, timedPark}
import fr.linkit.engine.internal.debug.{Debugger, TaskPausedStep}

import java.io.Closeable
import java.util.concurrent._
import java.util.concurrent.locks.{Lock, LockSupport, ReentrantLock}
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.control.NonFatal

/**
 * This class handles a FixedThreadPool from Java executors, excepted that the used threads can be busy about
 * executing other tasks contained in the pool instead of stupidly waiting in an object monitor
 * <h2> Problem </h2>
 * <p>
 * Let's say that we have a pool of 3 threads that handle deserialization and packet injections. <br>
 * (see [[fr.linkit.api.gnom.packet.traffic.PacketInjectable]] for further details about injection)
 * Then suddenly, for a reason that can often appear, every threads of the pool are waiting to receipt another packet.<br>
 * The waited packet will effectively be downloaded by the [[PacketReaderThread]],
 * but it could not be deserialized and injected because all the thread are currently waiting for an injection.<br>
 * This way, a kind of deadlock will occur because each threads are waiting for their packet to be injected,
 * and there is no free thread in the pool that would process the packet injection<br>
 * </p>
 *
 * <h3> Pseudo code : </h3>
 *
 * Here is an illustration of a normal execution.<br>
 * Where the first thread is waiting for a packet to be received,
 * and where a second thread is injecting the packet in the channel.
 *
 * <u>Thread 1 : waiting for a packet to be received</p>
 * {{{
 *      val channel = connection.getInjectable(x, ChannelScope.y, SyncPacketChannel)
 *      val nextPacket = channel.nextPacket()
 *      println("A new packet has been received !")
 *      // Process nextPacket....
 * }}}
 *
 * <u>Thread 2 : injecting the next packet that concerns the channel, handled by the thread 1.</u><br>
 * {{{
 *      val injectable = //Retrieves the needed injectable, stored with 'x' identifier
 *      val packetInjection = //Get the concerned injection object
 *      injectable.injectPacket(packetInjection) //The injection will notify the first thread.
 *      //println("Another packet injection has been performed !")
 * }}}
 *
 * <h2> Solution </h2>
 * <p>
 * In a normal execution, where a second thread is free to notify the first thread,
 * the two prints would be done successfully
 * </p>
 * <p>
 * Therefore, if the second thread were not able to handle the injection, because it would be busy to
 * execute another task submitted to the thread pool, the first thread could not be
 * notified, and will wait until a thread is free to process the injection.<br>
 * But we have to rely on the fact that the first thread is doing noting.<br>
 * But, are we saying that we have a thread that is stupidly waiting for a packet to be provided, and will do
 * absolutely nothing until he does not received its wants,
 * where he can take the time he is sleeping for executing other
 * tasks in the pool ? and thus consequently injecting his own packet to unlock itself ? What a lazy thread !
 * </p>
 * <p>
 * The Busy thread system will save this lost time in order to fluidify task execution,
 * and make one thread able to handle multiple tasks even if a task needs to wait a Linkit resource.
 *
 * @see [[WorkerBlockingQueue]] for busy waitings example.
 * @param initialThreadCount The number of threads the pool will contain (can ba changed with [[setThreadCount]].
 * */
abstract class AbstractWorkerPool(val name: String) extends WorkerPool with Closeable {

    protected val workers: ListBuffer[Worker] = ListBuffer.empty
    protected var closed                      = false

    @volatile private var taskCount = 0

    protected def nextTaskCount: Int = {
        taskCount += 1
        taskCount
    }

    override def close(): Unit = {
        closed = true
    }

    /**
     * Submits a task to the executor thread pool.
     * The task will then be handled directly or right after a thread is looking for other task to schedule.
     *
     * @throws IllegalStateException if the pool is closed
     * @param task the task to execute in the thread pool
     * */
    override def runLaterControl[A](@workerExecution task: => A): WorkerTask[A] = {
        if (closed)
            throw new IllegalStateException("Attempted to submit a task in a closed thread pool !")

        var runnable: Runnable = null
        val submittedTaskID    = nextTaskCount
        val currentTask        = currentTaskWithController.orNull
        val childTask          = SimpleAsyncTask[A](submittedTaskID, currentTask)(Try(task))
        val currentEngine      = ExecutorEngine.currentEngine
        runnable = () => {
            val oldEngine = ExecutorEngine.currentEngine
            if (currentEngine != null)
                ExecutorEngine.setCurrentEngine(currentEngine)

            try {
                currentWorker
                    .getController
                    .runTask(childTask)
            } catch {
                case e: Throwable =>
                    e.printStackTrace()
            }
            if (oldEngine != null)
                ExecutorEngine.setCurrentEngine(oldEngine) //return to the previous engine
        }
        postTask(runnable)

        //If there is one busy thread that is waiting for a new task to be performed,
        //It would instantly execute the current task.
        unparkBusyThread()
        childTask
    }

    protected def post(runnable: Runnable): Unit

    protected def countRemainingTasks: Int

    private def postTask(runnable: Runnable): Unit = {
        post(runnable)
        val count = countRemainingTasks
        if (count > 5) {
            AppLoggers.Worker.warn(s"Worker Pool '$name' is suffocating! there are $count tasks remaining to execute.")
        }
    }

    protected def addWorker(worker: Worker): Unit = {
        workers.synchronized {
            workers += worker
        }
    }

    override def ensureCurrentThreadOwned(): Unit = {
        ensureCurrentThreadOwned(s"Current thread is not owned by worker pool '$name'")
    }

    override def ensureCurrentThreadOwned(msg: String): Unit = {
        if (!isCurrentThreadOwned)
            throw IllegalThreadException(msg)
    }

    override def isCurrentThreadOwned: Boolean = {
        currentPool.exists(_ eq this)
    }

    override def execute(runnable: Runnable): Unit = {
        if (isCurrentThreadOwned) {
            runnable.run()
            return
        }
        runLaterControl(runnable.run())
    }

    override def reportFailure(cause: Throwable): Unit = {
        if (!isCurrentThreadWorker)
            return
        currentWorker
            .getController
            .getCurrentTask
            .get
            .notifyNestThrow(cause)
    }


    override def pauseCurrentTask(): Unit = pauseCurrentTask(new ReentrantLock())

    /**
     * pauses the current task and look for executing other pending tasks while this task is paused
     *
     * @param lock a lock to acquire until the current task is marked as paused.
     *
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * */
    @workerExecution
    override def pauseCurrentTask(lock: Lock): Unit = {
        AppLoggers.Worker.trace("Pausing current task ...")
        lock.lock()
        AppLoggers.Worker.trace("Locked on given lock")
        ensureCurrentThreadOwned()
        val worker      = currentWorker.getController
        val currentTask = worker.getCurrentTask.get
        val taskLock = currentTask.lock
        taskLock.lock()
        AppLoggers.Worker.trace("Locked on task")
        Debugger.push(TaskPausedStep(currentTask.taskID))
        currentTask.setPaused()
        lock.unlock()
        taskLock.unlock()
        executeRemainingTasksWhilePaused()
        if (!currentTask.isPaused) return

        worker.execWhileCurrentTaskPaused(LockSupport.park(currentTask), currentTask.isPaused) { _ =>
            executeRemainingTasksWhilePaused()
        }
        Debugger.pop()
    }

    final def haveMoreTasks: Boolean = countRemainingTasks > 0

    //TODO should return a ThreadTask.
    protected def pollTask: Runnable

    protected def takeTask: Runnable

    /**
     * Keep the current thread busy with task execution for at least
     * x milliseconds.
     *
     * @param timeoutMillis the number of milliseconds the thread must be busy.
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * */
    override def pauseCurrentTaskForAtLeast(timeoutMillis: Long): Unit = {
        val worker      = currentWorker.getController
        val currentTask = worker.getCurrentTask.get

        if (timeoutMillis == 0) {
            pauseCurrentTask()
            return
        }
        if (timeoutMillis <= 0)
            throw new IllegalArgumentException("timeoutMillis is negative.")

        def pauseCurrentTaskTimed(): Long = {
            var busiedMillis: Long = 0
            while (busiedMillis <= timeoutMillis) {
                val t0 = now()
                takeTask.run()
                val t1 = now()
                busiedMillis += (t1 - t0)
            }
            busiedMillis
        }

        Debugger.push(TaskPausedStep(currentTask.taskID, timeoutMillis))
        currentTask.setPaused()

        var toWait = timeoutMillis - pauseCurrentTaskTimed()
        worker.execWhileCurrentTaskPaused(timedPark(currentTask, toWait), toWait > 0) { waited =>
            toWait -= waited
            if (toWait <= 0) {
                currentTask.setContinue()
            } else toWait -= pauseCurrentTaskTimed()
        }

        currentTask.setContinue()
        Debugger.pop()
        AppLoggers.Worker.debug(s"task ${currentTask.taskID} is continuing...")
    }

    def ensureCurrentThreadNotOwned(): Unit = {
        ensureCurrentThreadNotOwned(s"Current thread is owned by worker pool '$name'")
    }

    def ensureCurrentThreadNotOwned(msg: String): Unit = {
        if (isCurrentThreadOwned)
            throw IllegalThreadException(msg)
    }

    /**
     * Creates a blocking queue that keep busy its thread instead of make it waiting
     * the provided queue will use the busy threading system for concurrent operations such as
     * [[BlockingQueue#take()]]
     *
     * @tparam A the type of element the queue will contains
     * @return a [[WorkerBlockingQueue]]
     */
    override def newBusyQueue[A]: BlockingQueue[A] = {
        new WorkerBlockingQueue[A](this)
    }

    /**
     * The Task Execution Depth is an int value that determines the number of tasks
     * a thread is consequently executing.
     *
     * @return the task execution depth of the current thread
     * @throws IllegalThreadException if the current thread is not a [[Worker]]
     * */
    @workerExecution
    def currentTaskExecutionDepth: Int = {
        ensureCurrentThreadOwned(s"This action is only permitted to worker threads of thread pool $name")
        currentWorker.taskRecursionDepth
    }

    private def unparkBusyThread(): Unit = workers.synchronized {
        val sleepingWorker = workers.find(_.isSleeping)
        //AppLogger.vDebug(s"unparking busy thread ${sleepingWorker.orNull} (if null, no thread is sleeping)")
        sleepingWorker match {
            case None         => //no-op
            case Some(worker) => LockSupport.unpark(worker.thread)
        }
    }

    protected def threadCount: Int = workers.length

    private def executeRemainingTasksWhile(condition: => Boolean): Unit = if (haveMoreTasks) {
        ensureCurrentThreadOwned()

        val currentController = currentWorker.getController
        while (haveMoreTasks && condition) {
            val task = pollTask
            if (task != null) {
                currentController.runSubTask(task)
            }
        }
    }

    def executeRemainingTasks(): Unit = {
        executeRemainingTasksWhile(true)
    }

    private def executeRemainingTasksWhilePaused(): Unit = {
        val ct = currentTask.get
        executeRemainingTasksWhile(ct.isPaused)
    }

    override def runLater(task: => Unit): Unit = runLaterControl {
        try {
            task
        } catch {
            case NonFatal(e)  =>
                AppLoggers.Worker.error(s"Error in thread ${Thread.currentThread()}")
                e.printStackTrace()
            case e: Throwable =>
                e.printStackTrace()
                throw e
        }
    }
}