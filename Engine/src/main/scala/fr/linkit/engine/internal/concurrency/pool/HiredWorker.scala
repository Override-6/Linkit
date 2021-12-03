package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{WorkerPool, WorkerThreadController}

class HiredWorker(override val thread: Thread, override val pool: WorkerPool)
    extends AbstractWorker with WorkerThreadController {
}