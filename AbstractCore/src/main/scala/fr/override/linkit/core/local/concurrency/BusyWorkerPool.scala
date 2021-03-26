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

package fr.`override`.linkit.core.local.concurrency

import fr.`override`.linkit.api.local.concurrency.{IllegalThreadException, Procrastinator, workerExecution}
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool.{checkCurrentIsWorker, currentPool, workerThreadGroup}
import fr.`override`.linkit.core.local.system.ContextLogger

import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory, ThreadPoolExecutor}
import scala.util.control.NonFatal

/**
 * This class handles a FixedThreadPool from Java executors, excepted that the used threads can be busy about
 * executing other tasks contained in the pool instead of stupidly waiting in an object monitor
 * <h2> Problem </h2>
 * <p>
 * Let's say that we have a pool of 3 threads that handle deserialization and packet injections. <br>
 * (see [[fr.`override`.linkit.api.connection.packet.traffic.PacketInjectable]] for further details about injection
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
 * @param nThreads The number of threads the pool will contain.
 * */
class BusyWorkerPool(val nThreads: Int, name: String) extends AutoCloseable with Procrastinator {
    if (nThreads <= 0)
        throw new IllegalArgumentException(s"Worker pool '$name' must contain at least 1 thread, provided: '$nThreads'")

    private val factory: ThreadFactory = new RelayThread(_, name)
    private val executor = Executors.newFixedThreadPool(nThreads, factory).asInstanceOf[ThreadPoolExecutor]

    //The extracted workQueue of the executor which contains all the tasks to execute
    private val workQueue = executor.getQueue
    private var closed = false
    private val workersLocks = new WorkersLock
    @volatile private var activeThreads = 0
    private var threadID = 1

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
                task
            } catch {
                case NonFatal(e) => e.printStackTrace()
                case e if currentTaskExecutionDepth == 0 =>
                    ContextLogger.fatal(s"Caught fatal exception in thread pool '$name'. The JVM Will exit.")
                    e.printStackTrace()
                    System.exit(1)
            }
            activeThreads -= 1
        }
        executor.submit(runnable)
        //If there is one busy thread that is waiting for a new task to be performed,
        //It would instantly execute the current task.
        workersLocks.notifyOneBusyThread()
    }

    /**
     * @return the number of threads that are currently executing a task.
     * */
    def busyThreads: Int = activeThreads

    def setThreadCount(newCount: Int): Unit = {
        executor.setCorePoolSize(newCount)
        executor.setMaximumPoolSize(newCount)
        ContextLogger.trace(s"$name's core pool size is set to $newCount")
    }

    /**
     * Executes tasks contained in the workQueue
     * until the queue is empty or the condition returns false
     *
     * @throws IllegalThreadException if the current thread is not a [[RelayThread]]
     * @param condition the condition to check, the thread will continue to be busy with tasks
     *                  while the condition is true. (and while the thread have tasks to execute)
     * */
    def executeRemainingTasksWhile(condition: => Boolean): Unit = {
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
     * @throws IllegalThreadException if the current thread is not a [[RelayThread]]
     * @see [[BusyLock]]
     * */
    def executeRemainingTasks(lock: AnyRef = new Object, condition: => Boolean): Unit = {
        executeRemainingTasksWhile(condition)
        if (!condition)
            return
        lock.synchronized {
            if (condition) {
                workersLocks.addBusyLock(lock)
                lock.wait()
                workersLocks.removeLastBusyLock()
            }
        }
        if (condition) //We may still need to be busy
            executeRemainingTasks(lock, condition)
    }

    /**
     * Keep the current thread busy with task execution for at least
     * x milliseconds.
     *
     * @param lock   the lock that will handle monotirs
     * @param millis the number of milliseconds the thread must be busy.
     * @throws IllegalThreadException if the current thread is not a [[RelayThread]]
     * */
    def executeRemaingTasks(millis: Long, lock: AnyRef = new Object): Unit = {
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
                executeRemaingTasks(millis)
        }
    }

    def checkCurrentThreadOwned(): Unit = {
        checkCurrentThreadOwned(s"Current thread is not owned by worker pool '$name'")
    }

    def checkCurrentThreadOwned(msg: String): Unit = {
        if (!isCurrentThreadOwned)
            throw new IllegalThreadException(msg)
    }

    def checkCurrentThreadNotOwned(): Unit = {
        checkCurrentThreadOwned(s"Current thread is owned by worker pool '$name'")
    }

    def checkCurrentThreadNotOwned(msg: String): Unit = {
        if (isCurrentThreadOwned)
            throw new IllegalThreadException(msg)
    }

    def isCurrentThreadOwned: Boolean = {
        currentPool().exists(_ eq this)
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
     * @throws IllegalThreadException if the current thread is not a [[RelayThread]]
     * */
    @workerExecution
    def currentTaskExecutionDepth: Int = {
        checkCurrentIsWorker("This action is only permitted to relay threads")
        currentWorker.currentTaskExecutionDepth
    }

    /**
     * Casts the current thread as a [[RelayThread]]
     *
     * @return the instance of the current relay thread
     * @throws IllegalThreadException if the current thread is not a [[RelayThread]]
     * */
    @workerExecution
    private def currentWorker: RelayThread = {
        checkCurrentIsWorker()
        //After the check, we are sure that the current thread is a RelayThread
        currentThread.asInstanceOf[RelayThread]
    }

    /**
     * The representation of a java thread, extending from [[Thread]].
     * This class contains information that need to be stored into a specific thread class.
     * */
    private final class RelayThread private[BusyWorkerPool](target: Runnable, poolName: String)
        extends Thread(workerThreadGroup, target, s"$poolName's Thread#$threadID") {
        private[BusyWorkerPool] val ownerPool: BusyWorkerPool = BusyWorkerPool.this

        threadID += 1

        var currentTaskExecutionDepth = 0

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

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerPool#RelayThread]]
     * */
    def checkCurrentIsWorker(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed in a Packet Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[BusyWorkerPool#RelayThread]]
     * @param msg the message to complain with the exception
     * */
    def checkCurrentIsWorker(msg: String): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must be performed in a Packet Worker thread ! ($msg)")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerPool#RelayThread]]
     * */
    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed in a Packet Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[BusyWorkerPool#RelayThread]]
     * @param msg the message to complain with the exception
     * */
    def checkCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must not be performed in a Packet Worker thread ! ($msg)")
    }

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[BusyWorkerPool#RelayThread]]
     * */
    def isCurrentWorkerThread: Boolean = {
        currentThread.isInstanceOf[BusyWorkerPool#RelayThread]
    }

    /**
     * if the current thread is a relay thread, it would be busy with task execution while the provided
     * execution is true.
     * If the current thread is not a relay thread, it would do nothing.
     *
     * @param condition the condition to test
     * */
    def executeRemainingTasksWhile(condition: => Boolean): Unit = {
        ifCurrentWorkerOrElse(_.executeRemainingTasksWhile(condition), ())
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
    def executeRemainingTasks(lock: AnyRef, condition: => Boolean): Unit = {
        ifCurrentWorkerOrElse(_.executeRemainingTasks(lock, condition), if (condition) lock.synchronized(lock.wait()))
    }

    /**
     * if the current thread is a relay thread, it would be busy with task execution while the time
     * elapsed during the task execution is under the minTimeOut
     * If the current thread is not a relay thread, it would do nothing.
     *
     * @param minTimeOut the minimum time to keep busy
     * @param lock       the lock to handle
     * @see [[BusyWorkerPool.executeRemainingTasks]] for further details about the busy system.
     * */
    def smartKeepBusy(lock: AnyRef, minTimeOut: Long): Unit = {
        ifCurrentWorkerOrElse(_.executeRemaingTasks(minTimeOut), lock.synchronized(lock.wait(minTimeOut)))
    }

    /**
     * Toggles between two actions if the current thread is an instance of [[RelayThread]]
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
            case worker: BusyWorkerPool#RelayThread => Some(worker.ownerPool)
            case _ => None
        }
    }

}