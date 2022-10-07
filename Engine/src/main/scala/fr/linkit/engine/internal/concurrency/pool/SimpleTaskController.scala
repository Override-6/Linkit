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

import fr.linkit.api.internal.concurrency.{TaskController, WorkerTask, workerExecution}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.concurrency.pool.SimpleTaskController.ControlTicket

import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable

class SimpleTaskController extends TaskController {

    private val pausedTasksMap   = new mutable.HashMap[Int, ControlTicket]()
    private val pausedTasksQueue = mutable.PriorityQueue.empty[ControlTicket]((a, b) => a.task.taskID - b.task.taskID)
    private val lock             = new ReentrantLock()

    protected def tickets: mutable.PriorityQueue[ControlTicket] = {
        if (pausedTasksQueue.nonEmpty) {
            pausedTasksMap.filterInPlace((_, tick) => tick.task.isExecuting)
            return pausedTasksQueue.filter(_.task.isPaused)
        }
        pausedTasksQueue
    }

    @workerExecution
    override def pauseTask(): Unit = {
        //unlock condition as false means that we can be unlocked at any time.
        createControlTicket(false)
    }

    @workerExecution
    override def pauseTaskWhile(condition: => Boolean): Unit = {
        if (!condition)
            return
        createControlTicket(condition)
    }

    @workerExecution
    override def pauseTaskForAtLeast(millis: Long): Unit = {
        val lockDate = System.currentTimeMillis()
        createControlTicket(System.currentTimeMillis() - lockDate <= millis)
        pauseCurrentTask(millis)
    }

    override def wakeupNTask(n: Int): Unit = {
        lock.lock()
        val count = pausedTasksMap.size
        val x     = if (n > count || n < 0) count else n
        AppLoggers.Worker.trace(s"Waking up $count tasks...")
        for (_ <- 0 to x) {
            wakeupAnyTask()
        }
        lock.unlock()
    }

    override def toString: String = pausedTasksMap.mkString("SimpleWorkerController(", ",", ")")

    @workerExecution
    override def wakeupAnyTask(): Unit = {
        lock.lock()
        val tickets = this.tickets
        println(tickets)
        val opt     = tickets.find(_.shouldWakeup)
        AppLoggers.Worker.debug(s"wakeupAnyTask $this, $opt (${System.identityHashCode(this)})")
        if (opt.isEmpty) {
            lock.unlock()
            return
        }

        val ticket = opt.get
        wakeupWorkerTask(ticket.task)
        lock.unlock()
    }

    @workerExecution
    override def wakeupTasks(taskIds: Seq[Int]): Unit = {
        lock.lock()
        val tickets = this.tickets
        if (tickets.isEmpty) {
            lock.unlock()
            return
        }
        for (ticket <- tickets) {
            val task = ticket.task
            if (taskIds.contains(task.taskID)) {
                wakeupWorkerTask(task)
            }
        }
        lock.unlock()
    }

    @workerExecution
    override def wakeupWorkerTask(task: WorkerTask[_]): Unit = {
        lock.lock()
        if ((pausedTasksMap remove task.taskID).isEmpty) {
            lock.unlock()
            throw new NoSuchElementException(s"Provided thread is not handled by this controller ! (${task.getWorker.thread.getName})")
        }
        if (task.isPaused)
            task.continue()
        lock.unlock()
    }


    private def pauseCurrentTask(millis: Long): Unit = EngineWorkerPools.ensureCurrentIsWorker().pauseCurrentTaskForAtLeast(millis)

    protected def createControlTicket(pauseCondition: => Boolean): Unit = {
        val currentTask = EngineWorkerPools.currentTask.get
        val ticket = new ControlTicket(currentTask, pauseCondition)
        lock.lock()
        pausedTasksMap.put(currentTask.taskID, ticket)
        pausedTasksQueue += ticket
        lock.unlock()
        EngineWorkerPools.ensureCurrentIsWorker().pauseCurrentTask(lock)
    }

}

object SimpleTaskController {

    protected class ControlTicket(val task: WorkerTask[_], condition: => Boolean) {
        private val lock = task.lock

        def shouldWakeup: Boolean = {
            lock.lock()
            val cond = !condition
            lock.unlock()
            cond
        }

        override def toString: String = s"task: $task"

    }

}
