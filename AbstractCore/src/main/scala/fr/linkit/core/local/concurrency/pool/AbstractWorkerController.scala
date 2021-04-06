package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.{WorkerController, WorkerThread, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable

abstract class AbstractWorkerController[W <: WorkerThread] extends WorkerController[W] {

    private val entertainedThreads = new mutable.HashMap[Int, W]()

    @workerExecution
    override def waitTask(): Unit = {
        val worker      = currentWorker
        val currentTask = worker.currentTaskID
        entertainedThreads.put(currentTask, worker)
        waitCurrentTask()
    }

    @workerExecution
    override def waitTask(millis: Long): Unit = {
        val worker      = currentWorker
        val currentTask = worker.currentTaskID
        entertainedThreads.put(currentTask, worker)
        waitCurrentTask(millis)
    }

    override def notifyNThreads(n: Int): Unit = {
        for (_ <- 0 to n) {
            notifyAnyThread()
        }
    }

    @workerExecution
    override def notifyAnyThread(): Unit = {
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)
        val opt = entertainedThreads.headOption
        if (opt.isEmpty)
            return

        val entry  = opt.get
        val worker = entry._2
        val taskID = entry._1

        notifyWorkerTask(worker, taskID)
        entertainedThreads -= taskID
    }

    @workerExecution
    override def notifyThreadsTasks(taskIds: Int*): Unit = {
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)

        if (entertainedThreads.isEmpty) {
            AppLogger.error("THREADS ARE EMPTY !")
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.
        }

        for ((taskID, worker) <- entertainedThreads.clone()) {
            if (taskIds.contains(taskID)) {
                notifyWorkerTask(worker, taskID)
                entertainedThreads -= taskID
            }
        }
    }

    @workerExecution
    override def notifyWorkerTask(thread: W, taskID: Int): Unit = {
        if (!entertainedThreads.contains(taskID))
            throw new NoSuchElementException(s"Provided thread is not entertained by this entertainer ! (${thread.getName})")
        notifyWorker(thread, taskID)
    }

    def currentWorker: W

    def notifyWorker(worker: W, taskID: Int): Unit

    def waitCurrentTask(): Unit

    def waitCurrentTask(millis: Long): Unit

}
