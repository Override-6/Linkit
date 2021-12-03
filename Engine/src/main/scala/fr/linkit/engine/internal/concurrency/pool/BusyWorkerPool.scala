package fr.linkit.engine.internal.concurrency.pool

import java.util.concurrent.{LinkedBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}

import fr.linkit.api.internal.system.AppLogger

class BusyWorkerPool(initialThreadCount: Int, name: String) extends AbstractWorkerPool(name) {

    if (initialThreadCount <= 0)
        throw new IllegalArgumentException(s"Worker pool '$name' must contain at least 1 thread, provided: '$initialThreadCount'")

    //The extracted workQueue of the executor which contains all the tasks to execute
    private   val workQueue = new LinkedBlockingQueue[Runnable]()
    //private val choreographer = new Choreographer(this)
    protected val executor  = new ThreadPoolExecutor(initialThreadCount, initialThreadCount, 0, TimeUnit.MILLISECONDS, workQueue, getThreadFactory)

    override def haveMoreTasks: Boolean = !workQueue.isEmpty

    override protected def nextTask: Runnable = workQueue.take()

    def setThreadCount(newCount: Int): Unit = {
        executor.setMaximumPoolSize(newCount)
        AppLogger.trace(s"$name's core pool size is set to $newCount")
        executor.setCorePoolSize(newCount)
    }

    private def getThreadFactory: ThreadFactory = target => {
        val worker = new BusyWorkerThread(target, this, threadCount + 1)
        addWorker(worker)
        worker
    }

    override def close(): Unit = {
        super.close()
        executor.shutdownNow()
    }

    override protected def post(runnable: Runnable): Unit = executor.submit(runnable)
}
