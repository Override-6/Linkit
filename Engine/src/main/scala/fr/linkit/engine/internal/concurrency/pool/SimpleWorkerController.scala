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

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.WorkerPools.currentWorker
import fr.linkit.api.internal.concurrency.{AsyncTask, WorkerController, WorkerPools, workerExecution}
import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController.ControlTicket

import scala.collection.mutable

class SimpleWorkerController extends WorkerController {
    
    private val workingThreads = new mutable.HashMap[Int, ControlTicket]()
    
    private def tickets: mutable.HashMap[Int, ControlTicket] = {
        workingThreads.filterInPlace((_, tick) => tick.task.isExecuting)
        workingThreads.filter(_._2.task.isPaused)
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
        val worker      = currentWorker
        val currentTask = worker.getCurrentTask.get
        val lockDate    = System.currentTimeMillis()
        workingThreads.put(currentTask.taskID, new ControlTicket(currentTask, System.currentTimeMillis() - lockDate <= millis))
        pauseCurrentTask(millis)
    }
    
    override def wakeupNTask(n: Int): Unit = this.synchronized {
        val count = workingThreads.size
        val x     = if (n > count || n < 0) count else n
        for (_ <- 0 to x) {
            wakeupAnyTask()
        }
    }
    
    override def toString: String = workingThreads.mkString("SimpleWorkerController(", ",", ")")
    
    @workerExecution
    override def wakeupAnyTask(): Unit = this.synchronized {
        val opt = tickets.find(entry => entry._2.shouldWakeup)
        //AppLogger.debug(s"wakeupAnyTask $this (${System.identityHashCode(this)})")
        if (opt.isEmpty) {
            return
        }
        
        val entry  = opt.get
        val ticket = entry._2
        
        wakeupWorkerTask(ticket.task)
    }
    
    @workerExecution
    override def wakeupTasks(taskIds: Seq[Int]): Unit = this.synchronized {
        if (workingThreads.isEmpty)
            return
        
        for ((taskID, ticket) <- workingThreads.clone()) {
            if (taskIds.contains(taskID)) {
                wakeupWorkerTask(ticket.task)
            }
        }
    }
    
    @workerExecution
    override def wakeupWorkerTask(task: AsyncTask[_]): Unit = this.synchronized {
        val taskID = task.taskID
        if (!workingThreads.contains(taskID))
            throw new NoSuchElementException(s"Provided thread is not handled by this controller ! (${task.getWorker.thread.getName})")
        
        workingThreads -= taskID
        if (task.isPaused)
            task.continue()
    }
    
    private def pauseCurrentTask(): Unit = {
        WorkerPools.ensureCurrentIsWorker().pauseCurrentTask()
    }
    
    private def pauseCurrentTask(millis: Long): Unit = WorkerPools.ensureCurrentIsWorker().pauseCurrentTaskForAtLeast(millis)
    
    private def createControlTicket(pauseCondition: => Boolean): Unit = {
        this.synchronized {
            val currentTask = WorkerPools.currentTask.get
            workingThreads.put(currentTask.taskID, new ControlTicket(currentTask, pauseCondition))
        }
        pauseCurrentTask()
    }
    
}

object SimpleWorkerController {
    
    private class ControlTicket(val task: AsyncTask[_], condition: => Boolean) {
        
        def shouldWakeup: Boolean = !condition
    
        override def toString: String = s"task: $task"
        
    }
    
}
