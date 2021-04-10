package fr.linkit.core.local.concurrency.pool

class DedicatedWorkerController(pool: BusyWorkerPool) extends AbstractWorkerController[BusyWorkerThread] {

    def this() {
        this(BusyWorkerPool.ensureCurrentIsWorker())
    }

    override def currentWorker: BusyWorkerThread = BusyWorkerPool.currentWorker

    override def notifyWorker(worker: BusyWorkerThread, taskID: Int): Unit = BusyWorkerPool.unpauseTask(worker, taskID)

    override def waitCurrentTask(): Unit = pool.pauseCurrentTask()

    override def waitCurrentTask(millis: Long): Unit = pool.pauseCurrentTaskForAtLeast(millis)
}
