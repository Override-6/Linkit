package fr.linkit.engine.internal.concurrency

import fr.linkit.api.internal.concurrency.{Worker, WorkerPool}

class VirtualWorker(val pool  : WorkerPool,
                    val thread: Thread,
                    val taskID: Int) extends Worker {
}
