package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{WorkerPool, InternalWorkerThread}
import fr.linkit.engine.internal.concurrency.SimpleAsyncTask

import scala.util.Failure

class HiredWorker(override val thread: Thread, override val pool: WorkerPool)
        extends AbstractWorker with InternalWorkerThread { that =>

    private final val rootTask = new SimpleAsyncTask[Nothing](-1, null, () => Failure[Nothing](null)) {
        setWorker(that)
    }

    override def getCurrentTask: Some[ThreadTask] = super.getCurrentTask match {
        case Some(value) => Some(value)
        case None        => Some(rootTask)
    }

}
