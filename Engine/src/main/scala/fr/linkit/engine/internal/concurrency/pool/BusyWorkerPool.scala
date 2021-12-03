package fr.linkit.engine.internal.concurrency.pool

import java.util.concurrent.ThreadFactory

class BusyWorkerPool(initialThreadCount: Int, name: String) extends AbstractWorkerPool(initialThreadCount, name) {
    override protected def getThreadFactory: ThreadFactory = target => {
        val worker = new BusyWorkerThread(target, this, threadCount + 1)
        workers.synchronized {
            workers += worker
        }
        worker
    }
}
