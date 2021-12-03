package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.WorkerPool
import fr.linkit.api.internal.concurrency.WorkerPools.workerThreadGroup

class BusyWorkerThread(target: Runnable,
                       override val pool: WorkerPool,
                       tid: Int) extends Thread(workerThreadGroup, target, s"${pool.name}'s Thread#$tid") with AbstractWorker {
    override val thread: Thread = this
}

