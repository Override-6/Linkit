package fr.`override`.linkit.core.local.concurrency

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object SyncExecutionContext extends ExecutionContext {

    val threadGroup = new ThreadGroup("Relay Execution Context")

    private val queue: BlockingDeque[Runnable] = new LinkedBlockingDeque()

    private val worker = new Thread(threadGroup, () => {
        while (true) try {
            val action = queue.takeLast()
            action.run()
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
    })

    worker.setName("Relay Async Execution Worker")
    worker.start()

    override def execute(runnable: Runnable): Unit = queue.addFirst(runnable)

    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
}
