package fr.linkit.engine.internal.concurrency.pool

import java.util.concurrent.LinkedBlockingQueue

import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.internal.concurrency.SimpleAsyncTask

class HiringBusyWorkerPool(name: String) extends AbstractWorkerPool(name) {

    private val workQueue = new LinkedBlockingQueue[Runnable]()

    def hireCurrentThread(): Unit = {
        val currentThread = Thread.currentThread()
        val worker        = new HiredWorker(currentThread, this)
        WorkerPools.bindWorker(currentThread, worker)
        addWorker(worker)
    }

    override protected def post(runnable: Runnable): Unit = {
        workQueue.put(runnable)
    }

    override def haveMoreTasks: Boolean = {
        !workQueue.isEmpty
    }

    override protected def nextTask: Runnable = {
        workQueue.take()
    }
}