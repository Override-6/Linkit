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

package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.{IllegalThreadException, Procrastinator, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool._
import fr.linkit.core.local.concurrency.{currentThread, now, timedPark}

import java.util.concurrent.locks.LockSupport
import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory, ThreadPoolExecutor}
import scala.collection.mutable.ListBuffer

/**
 * This class handles a FixedThreadPool from Java executors, excepted that the used threads can be busy about
 * executing other tasks contained in the pool instead of stupidly waiting in an object monitor
 * <h2> Problem </h2>
 * <p>
 * Let's say that we have a pool of 3 threads that handle deserialization and packet injections. <br>
 * (see [[fr.linkit.api.connection.packet.traffic.PacketInjectable]] for further details about injection
 * Then suddenly, for a reason that can often appear, every threads of the pool are waiting to receipt another packet.<br>
 * The waited packet will effectively be downloaded by the [[PacketReaderThread]],
 * but it could not be deserialized and injected because all the thread are currently waiting for an injection.<br>
 * This way, a kind of deadlock will occur because each threads are waiting for their packet to be injected,
 * and there is no free thread that would process the packets in order to notify other threads that their packet has been effectively received.<br>
 * </p>
 *
 * <h3> Pseudo code : </h3>
 *
 * Here is an illustration of a normal execution, where a thread is waiting for a packet to be received,
 * and where a second thread is injecting the packet and notifying the first thread that a packet has been received.
 *
 * <u>Thread 1 : waiting for a packet to be received</p>
 * {{{
 *      val channel = relay.getInjectable(x, ChannelScope.y, SyncPacketChannel)
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
 *      println("Another packet injection has been performed !")
 * }}}
 *
 * <h2> Solution </h2>
 * <p>
 * In a normal execution, where a second thread is free to notify the first thread,
 * the two prints would be done at same time
 * </p>
 * <p>
 * Therefore, if the second thread were not able to handle the injection, because it would be busy to
 * execute another task submitted to thread pool, the first thread could not be
 * notified, and will wait until a thread is free to process the injection.<br>
 * But we have to rely on the fact that the first thread is doing noting.<br>
 * So, we have a thread that is waiting for a packet to be provided, and will do
 * absolutely nothing until he does not received his wants,
 * where he can take the time he is sleeping for executing other
 * tasks in the pool ? What a lazy thread !
 * </p>
 * <p>
 * The Busy thread system will save this lost time in order to fluidify task execution,
 * and make one thread able to handle multiple tasks when a task need to wait something.
 *
 * @see [[BusyBlockingQueue]] for busy waitings example.
 * @param initialThreadCount The number of threads the pool will contain (can ba changed with [[setThreadCount]].
 * */
class BusyWorkerPool(initialThreadCount: Int, val name: String) extends AutoCloseable with Procrastinator {

    if (initialThreadCount <= 0)
        throw new IllegalArgumentException(s"Worker pool '$name' must contain at least 1 thread, provided: '$initialThreadCount'")

    //private val choreographer = new Choreographer(this)
    private val executor = Executors.newFixedThreadPool(initialThreadCount, getThreadFactory).asInstanceOf[ThreadPoolExecutor]

    //The extracted workQueue of the executor which contains all the tasks to execute
    private val workQueue = executor.getQueue
    private val workers   = ListBuffer.empty[BusyWorkerThread]
    private var closed    = false

    //additional values for debugging
    @volatile private var activeThreads = 0
    @volatile private var taskCount     = 0
    private val workTaskIds             = ListBuffer.empty[Int]

    override def close(): Unit = {
        closed = true
        executor.shutdownNow()
    }

    /**
     * Submits a task to the executor thread pool.
     * The task will then be handled directly or right after a thread is looking for other task to schedule.
     *
     * @throws IllegalStateException if the pool is closed
     * @param task the task to execute in the thread pool
     * */
    override def runLater(@workerExecution task: => Unit): Unit = {
        if (closed)
            throw new IllegalStateException("Attempted to submit a task in a closed thread pool !")

        var runnable: Runnable = null
        taskCount += 1
        val submittedTaskID = taskCount
        runnable = () => {
            val rootExecution = currentTaskExecutionDepth == 0
            if (rootExecution)
                activeThreads += 1
            try {
                AppLogger.warn(s"${currentTasksId} <> ($activeThreads / ${threadCount}) TASK $submittedTaskID TAKEN FROM POOL $name")
                currentWorker.pushTaskID(submittedTaskID)
                task
                currentWorker.removeTaskID(submittedTaskID)
            } catch {
                case e                  =>
                    AppLogger.fatal(s"${currentTasksId} <> Caught fatal exception in thread pool '$name'. The JVM Will exit.")
                    AppLogger.printStackTrace(e)
                    System.exit(1)
                case e if rootExecution =>
                    AppLogger.fatal(s"${currentTasksId} <> Caught fatal exception in thread pool '$name'. The JVM Will exit.")
                    AppLogger.printStackTrace(e)
                    System.exit(1)
            }
            if (rootExecution)
                activeThreads -= 1
            workTaskIds.synchronized {
                workTaskIds -= submittedTaskID
                val tasks = workTaskIds.toString()
                AppLogger.warn(s"${currentTasksId} <> ($activeThreads / ${threadCount}) TASK ACCOMPLISHED ($submittedTaskID) ($tasks).")
            }
        }
        AppLogger.warn(s"${currentTasksId} <> ($activeThreads / ${threadCount}) Task $submittedTaskID will be submitted...")

        val tasks = workTaskIds.synchronized {
            AppLogger.discoverLines(0, 6)
            workTaskIds += submittedTaskID
            workTaskIds.toString()
        }
        executor.submit(runnable)
        AppLogger.warn(s"${currentTasksId} <> ($activeThreads / ${threadCount}) TASK $submittedTaskID SUBMIT TO POOL $name, TOTAL TASKS : $tasks ($workQueue) (${System.identityHashCode(workQueue)}), $this")
        //If there is one busy thread that is waiting for a new task to be performed,
        //It would instantly execute the current task.
        unparkBusyThread()
    }

    override def ensureCurrentThreadOwned(): Unit = {
        ensureCurrentThreadOwned(s"Current thread is not owned by worker pool '$name'")
    }

    override def ensureCurrentThreadOwned(msg: String): Unit = {
        if (!isCurrentThreadOwned)
            throw new IllegalThreadException(msg)
    }

    override def isCurrentThreadOwned: Boolean = {
        currentPool().exists(_ eq this)
    }

    def taskParkingThreads: Iterable[BusyWorkerThread] = {
        workers.filter(_.isParkingForWorkflow)
    }

    def countRemainingTasks: Int = workQueue.size()

    /**
     * @return the number of threads that are currently executing a task.
     * */
    def busyThreads: Int = activeThreads

    def setThreadCount(newCount: Int): Unit = {
        executor.setMaximumPoolSize(newCount)
        executor.setCorePoolSize(newCount)
        AppLogger.trace(s"$name's core pool size is set to $newCount")
    }

    /**
     * Keep executing tasks contained in the workQueue while
     * it is full and the provided condition is true.
     * If the condition keeps being true,
     * but there is no task that can be processed, the thread will wait
     * on the lock until he gets notified by the user or by the [[runLater()]] method.
     *
     * in plain language, this method will make the thread execute tasks
     * while the provided condition is true.
     * or, if the thread can't process other tasks, will wait until a task get submitted.
     *
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * */
    def waitCurrentTask(): Unit = {
        AppLogger.error(s"${currentTasksId} <> This Thread will execute remaining tasks or wait.")
        AppLogger.discoverLines(0, 7)
        executeRemainingTasks()

        currentWorker.workflowLoop(LockSupport.park()) { _ =>
            executeRemainingTasks()
        }
        AppLogger.error(s"${currentTasksId} <> executeRemainingTasksOrWait just ended.")
    }

    /**
     * Keep the current thread busy with task execution for at least
     * x milliseconds.
     *
     * @param millis the number of milliseconds the thread must be busy.
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * */
    def waitCurrentTaskForAtLeast(millis: Long): Unit = {
        ensureCurrentThreadOwned()

        var busiedMillis: Long = 0
        while (!workQueue.isEmpty && busiedMillis <= millis) {
            val t0 = now()
            workQueue.take().run()
            val t1 = now()
            busiedMillis += (t1 - t0)
        }

        var toWait = millis - busiedMillis
        while (toWait > 0) {
            currentWorker.workflowLoop(timedPark(toWait)) { waited =>
                toWait -= waited
            }
        }
    }

    def checkCurrentThreadNotOwned(): Unit = {
        checkCurrentThreadNotOwned(s"Current thread is owned by worker pool '$name'")
    }

    def checkCurrentThreadNotOwned(msg: String): Unit = {
        if (isCurrentThreadOwned)
            throw new IllegalThreadException(msg)
    }

    /**
     * Creates a blocking queue that keep busy his thread instead of make it waiting
     * the provided queue will use the busy threading system for concurrent operations such as
     * [[BlockingQueue#take()]]
     *
     * @tparam A the type of element the queue will contains
     * @return a [[BusyBlockingQueue]]
     */
    def newBusyQueue[A]: BlockingQueue[A] = {
        new BusyBlockingQueue[A](this)
    }

    /**
     * The Task Execution Depth is an int value that determines the number of tasks
     * a thread is consequently executing.
     *
     * @return the task execution depth of the current thread
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * */
    @workerExecution
    def currentTaskExecutionDepth: Int = {
        ensureCurrentThreadOwned(s"This action is only permitted to relay threads of thread pool $name")
        currentWorker.taskRecursionDepth
    }

    private def unparkBusyThread(): Unit = workers.synchronized {
        workers.find(_.isWaitingForRecursiveTask) match {
            case Some(thread) =>
                AppLogger.error(s"${thread.getName} <- This thread will be unparked because a new task is ready to be executed.")
                LockSupport.unpark(thread)
            case None         => //no-op
        }
    }

    private def threadCount: Int = workers.length

    private def getThreadFactory: ThreadFactory = target => {
        val worker = new BusyWorkerThread(target, this, threadCount + 1)
        workers.synchronized {
            workers += worker
        }
        worker
    }

    /**
     * Executes all tasks contained in the workQueue
     * until the queue is empty or the condition returns false
     *
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * */
    private def executeRemainingTasks(): Unit = {
        ensureCurrentThreadOwned()
        AppLogger.debug(s"EXECUTING ALL REMAINING TASKS $workQueue (${System.identityHashCode(workQueue)}), $this")
        AppLogger.debug(s"workQueue.isEmpty = ${workQueue.isEmpty}")
        AppLogger.debug(s"currentWorker.currentTaskIsWaiting = ${currentWorker.currentTaskIsWaiting()}")
        while (!workQueue.isEmpty && currentWorker.currentTaskIsWaiting()) {
            val task = workQueue.poll()
            if (task != null) {
                currentWorker.taskRecursionDepthCount += 1
                task.run()
                currentWorker.taskRecursionDepthCount -= 1
            }
        }
        AppLogger.error("Exit executeRemainingTasks...")
    }
}

object BusyWorkerPool {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Relay Worker")

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread is not a relay worker thread, this would mean that
     * we are not running into a thread that is owned by the Relay concurrency system. Therefore, the action
     * may be performed in the current thread
     *
     * @param action the action to perform
     * */
    def runLaterOrHere(action: => Unit): Unit = {
        ifCurrentWorkerOrElse(_.runLater(action), action)
    }

    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def runLaterInCurrentPool(@workerExecution action: => Unit): Unit = {
        val pool = ensureCurrentIsWorker("Could not run request action because current thread does not belong to any worker pool")
        pool.runLater(action)
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(): BusyWorkerPool = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed by a Worker thread !")
        currentPool().get
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(msg: String): BusyWorkerPool = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must be performed by a Worker thread ! ($msg)")
        currentPool().get
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed by a Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def checkCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must not be performed by a Worker thread ! ($msg)")
    }

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[BusyWorkerThread]]
     * */
    def isCurrentWorkerThread: Boolean = {
        currentThread.isInstanceOf[BusyWorkerThread]
    }

    /**
     * Toggles between two actions if the current thread is an instance of [[BusyWorkerThread]]
     *
     * @param ifCurrent The action to process if the current thread is a relay worker thread.
     *                  The given entry is the current thread pool
     * @param orElse    the action to process if the current thread is not a relay worker thread.
     *
     * */
    def ifCurrentWorkerOrElse[A](ifCurrent: BusyWorkerPool => A, orElse: => A): A = {
        val pool = currentPool()

        if (pool.isDefined) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
    }

    /**
     * @return Some if the current thread is a member of a [[BusyWorkerPool]], None instead
     * */
    def currentPool(): Option[BusyWorkerPool] = {
        currentThread match {
            case worker: BusyWorkerThread => Some(worker.pool)
            case _                        => None
        }
    }

    @workerExecution
    def currentWorker: BusyWorkerThread = {
        currentThread match {
            case worker: BusyWorkerThread => worker
            case _                        => throw new IllegalThreadException("Not a worker thread.")
        }
    }

    def notifyTask(thread: BusyWorkerThread, taskID: Int): Unit = {
        thread.stopWorkflowLoop(taskID)
    }

    def currentTasksId: String = {
        if (isCurrentWorkerThread)
            currentWorker.tasksId
        else "?"
    }

}