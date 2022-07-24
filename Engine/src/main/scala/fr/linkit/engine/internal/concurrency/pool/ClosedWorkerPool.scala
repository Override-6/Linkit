package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{Worker, WorkerPools}

import java.util.concurrent.LinkedBlockingQueue

class ClosedWorkerPool(initialThreadCount: Int, name: String) extends AbstractWorkerPool(name) {
    
    if (initialThreadCount < 0)
        throw new IllegalArgumentException(s"initialThreadCount < 0")
    
    //The extracted workQueue of the executor which contains all the tasks to execute
    private val workQueue                         = new LinkedBlockingQueue[Runnable]()
    private val workerFactory: Runnable => Worker = target => {
        val worker = new BusyWorkerThread(target, this, threadCount + 1)
        addWorker(worker)
        worker
    }
    setThreadCount(initialThreadCount)
    
    override protected def countRemainingTasks: Int = workQueue.size()
    
    override protected def pollTask: Runnable = workQueue.poll()
    override protected def takeTask: Runnable = workQueue.take()
    
    def setThreadCount(newCount: Int): Unit = {
        if (workers.size > newCount)
            throw new IllegalArgumentException(s"newCount < workers.size ($newCount < ${workers.size})")
        for (_ <- 0 until newCount - workers.size) {
            val worker = workerFactory(() => waitingRoom())
            worker.thread.start()
        }
    }
    
    override def close(): Unit = {
        super.close()
        val workerCount = workers.size
        workers.clear()
        for (_ <- 0 to workerCount)
        //all threads waiting to execute another tasks will see a null task was submit & that super.closed = true so they'll stop executing
            workQueue.add(null)
    }
    
    override protected def post(runnable: Runnable): Unit = workQueue.offer(runnable)
    
    private def waitingRoom(): Unit = {
        val self = WorkerPools.currentWorker.asInstanceOf[BusyWorkerThread]
        self.execWhileCurrentTaskPaused(workQueue.take(), !closed)(_.run())
    }
}
