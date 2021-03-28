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

import fr.linkit.api.local.concurrency.{IllegalThreadException, Procrastinator, workerExecution}
import fr.linkit.core.local.concurrency.BusyWorkerPool.{checkCurrentIsWorker, currentPool, workerThreadGroup}
import fr.linkit.core.local.system.AppLogger

import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory, ThreadPoolExecutor}
import scala.util.control.NonFatal

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
    private val factory: ThreadFactory = new WorkerThread(_, name)
    private val executor               = Executors.newFixedThreadPool(initialThreadCount, factory).asInstanceOf[ThreadPoolExecutor]

    //The extracted workQueue of the executor which contains all the tasks to execute
    private val workQueue               = executor.getQueue
    private val workersLocks            = new WorkersLock
    private           var closed        = false
    private           var threadID      = 1
    @volatile private var activeThreads = 0

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
        runnable = () => {
            activeThreads += 1
            try {
                AppLogger.warn(s"TASK TAKEN FROM POOL $name, ($activeThreads / ${threadID - 1}), TOTAL TASKS : $workQueue")
                task
            } catch {
                case NonFatal(e)                         => e.printStackTrace()
                case e if currentTaskExecutionDepth == 0 =>
                    AppLogger.fatal(s"Caught fatal exception in thread pool '$name'. The JVM Will exit.")
                    e.printStackTrace()
                    System.exit(1)
            }
            activeThreads -= 1
            AppLogger.warn(s"TASK ACCOMPLISHED. ($workQueue) ($activeThreads / ${threadID - 1})")
        }
        executor.submit(runnable)
        AppLogger.warn(s"TASK SUBMIT TO POOL $name, TOTAL TASKS : $workQueue")
        //If there is one busy thread that is waiting for a new task to be performed,
        //It would instantly execute the current task.
        workersLocks.notifyOneBusyThread()
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
     * Executes tasks contained in the workQueue
     * until the queue is empty or the condition returns false
     *
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * @param condition the condition to check, the thread will continue to be busy with tasks
     *                  while the condition is true. (and while the thread have tasks to execute)
     * */
    def executeRemainingTasks(condition: => Boolean): Unit = {
        checkCurrentIsWorker()

        while (!workQueue.isEmpty && condition) {
            val task = workQueue.poll()
            if (task != null) {
                currentWorker.currentTaskExecutionDepth += 1
                task.run()
                currentWorker.currentTaskExecutionDepth -= 1
            }
        }
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
     * Using a [[BusyLock]] is highly recommended to keep a thread busy
     *
     * @param lock      the reference to link a monitor lock if needed
     * @param condition the condition
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * @see [[BusyLock]]
     * */
    def executeRemainingTasksWhile(condition: => Boolean, lock: AnyRef = new Object): Unit = {
        executeRemainingTasks(condition)
        if (!condition)
            return
        lock.synchronized {
            if (condition) {
                workersLocks.addBusyLock(lock)
                //currentWorker.inBusyLock = true
                lock.wait()
                //currentWorker.inBusyLock = false
                workersLocks.removeLastBusyLock()
            }
        }
        if (condition) //We may still need to be busy
            executeRemainingTasksWhile(condition, lock)
    }

    /**
     * Keep the current thread busy with task execution for at least
     * x milliseconds.
     *
     * @param lock   the lock that will handle monotirs
     * @param millis the number of milliseconds the thread must be busy.
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * */
    def timedExecuteRemainingTasks(millis: Long, lock: AnyRef = new Object): Unit = {
        checkCurrentIsWorker()

        var totalProvided: Long = 0
        while (!workQueue.isEmpty && totalProvided <= millis) {
            val t0 = now()
            workQueue.take().run()
            val t1 = now()
            totalProvided += (t1 - t0)
        }
        val toWait = millis - totalProvided
        if (toWait > 0) {
            workersLocks.addBusyLock(lock)
            val waited = timedWait(lock, toWait)
            workersLocks.removeLastBusyLock()
            if (waited < toWait)
                timedExecuteRemainingTasks(millis)
        }
    }

    def checkCurrentThreadNotOwned(): Unit = {
        ensureCurrentThreadOwned(s"Current thread is owned by worker pool '$name'")
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
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * */
    @workerExecution
    def currentTaskExecutionDepth: Int = {
        checkCurrentIsWorker("This action is only permitted to relay threads")
        currentWorker.currentTaskExecutionDepth
    }

    /**
     * Casts the current thread as a [[WorkerThread]]
     *
     * @return the instance of the current relay thread
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * */
    @workerExecution
    private def currentWorker: WorkerThread = {
        checkCurrentIsWorker()
        //After the check, we are sure that the current thread is a WorkerThread
        currentThread.asInstanceOf[WorkerThread]
    }

    /**
     * The representation of a java thread, extending from [[Thread]].
     * This class contains information that need to be stored into a specific thread class.
     * */
    private[concurrency] final class WorkerThread private[BusyWorkerPool](target: Runnable, poolName: String)
            extends Thread(workerThreadGroup, target, s"$poolName's Thread#$threadID") {

        private[BusyWorkerPool] val ownerPool: BusyWorkerPool = BusyWorkerPool.this

        threadID += 1
        //choreographer.addThread(this)

        var currentTaskExecutionDepth = 0
        //var inBusyLock: Boolean = false
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
        val pool = checkCurrentIsWorker("Could not run request action because current thread does not belong to any worker pool")
        pool.runLater(action)
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerPool#WorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def checkCurrentIsWorker(): BusyWorkerPool = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed by a Worker thread !")
        currentPool().get
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerPool#WorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def checkCurrentIsWorker(msg: String): BusyWorkerPool = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must be performed by a Worker thread ! ($msg)")
        currentPool().get
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerPool#WorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed by a Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerPool#WorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def checkCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must not be performed by a Worker thread ! ($msg)")
    }

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[BusyWorkerPool#WorkerThread]]
     * */
    def isCurrentWorkerThread: Boolean = {
        currentThread.isInstanceOf[BusyWorkerPool#WorkerThread]
    }

    /**
     * if the current thread is a relay thread, it would be busy with task execution while the provided
     * execution is true.
     * If the current thread is not a relay thread, it would do nothing.
     *
     * @param condition the condition to test
     * */
    def executeRemainingTasks(condition: => Boolean): Unit = {
        ifCurrentWorkerOrElse(_.executeRemainingTasks(condition), ())
    }

    /**
     * if the current thread is a relay thread, it would be busy with task execution while the provided
     * execution is true.
     * If the current thread is not a relay thread, it would do nothing.
     *
     * @param condition the condition to test
     * @param lock      the lock to handle
     * @see [[BusyWorkerPool.executeRemainingTasks()]] for further details about the busy system.
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def executeRemainingTasksWhile(condition: => Boolean, lock: AnyRef = new Object): Unit = {
        val pool = checkCurrentIsWorker("executeRemainingTasksWhile must be processed by a worker thread !")
        pool.executeRemainingTasksWhile(condition, lock)
    }

    /**
     * if the current thread is a relay thread, it would be busy with task execution while the time
     * elapsed during the task execution is under the minTimeOut
     * If the current thread is not a relay thread, it would do nothing.
     *
     * @param minTimeOut the minimum time to keep busy
     * @param lock       the lock to handle
     * @see [[BusyWorkerPool.timedExecuteRemainingTasks]] for further details about the busy system.
     * */
    def smartKeepBusy(lock: AnyRef, minTimeOut: Long): Unit = {
        ifCurrentWorkerOrElse(_.timedExecuteRemainingTasks(minTimeOut), lock.synchronized(lock.wait(minTimeOut)))
    }

    /**
     * Toggles between two actions if the current thread is an instance of [[WorkerThread]]
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
            case worker: BusyWorkerPool#WorkerThread => Some(worker.ownerPool)
            case _                                   => None
        }
    }

}