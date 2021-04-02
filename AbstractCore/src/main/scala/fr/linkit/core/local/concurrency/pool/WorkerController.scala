package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.{currentTasksId, currentWorker}

import scala.collection.mutable

class WorkerController(pool: BusyWorkerPool) {

    private val entertainedThreads = new mutable.HashMap[Int, BusyWorkerThread]()

    def this() {
        this(BusyWorkerPool.ensureCurrentIsWorker())
    }

    @workerExecution
    def waitTask(): Unit = {
        val worker      = currentWorker
        val currentTask = worker.getCurrentTaskID
        entertainedThreads.put(currentTask, worker)
        pool.waitCurrentTask()
    }

    @workerExecution
    def waitTask(millis: Long): Unit = {
        val worker      = currentWorker
        val currentTask = worker.getCurrentTaskID
        entertainedThreads.put(currentTask, worker)
        pool.timedExecuteRemainingTasks(millis)
    }

    def notifyNThreads(n: Int): Unit = {
        for (_ <- 0 to n) {
            notifyFirstThread()
        }
    }

    @workerExecution
    def notifyFirstThread(): Unit = {
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)
        val opt = entertainedThreads.headOption
        if (opt.isEmpty)
            return

        val entry = opt.get
        val worker = entry._2
        val taskID = entry._1

        BusyWorkerPool.notifyTask(worker, taskID)
        entertainedThreads -= taskID
    }

    @workerExecution
    def notifyThreadsTasks(taskIds: Int*): Unit = {
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)

        if (entertainedThreads.isEmpty) {
            AppLogger.error("THREADS ARE EMPTY !")
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.
        }

        for ((taskID, worker) <- entertainedThreads.clone()) {
            if (taskIds.contains(taskID)) {
                BusyWorkerPool.notifyTask(worker, taskID)
                entertainedThreads -= taskID
            }
        }
    }

    @workerExecution
    def notifyThread(thread: BusyWorkerThread, taskID: Int): Unit = {
        if (!entertainedThreads.contains(taskID))
            throw new NoSuchElementException(s"Provided thread is not entertained by this entertainer ! (${thread.getName})")
        BusyWorkerPool.notifyTask(thread, taskID)
    }
}
