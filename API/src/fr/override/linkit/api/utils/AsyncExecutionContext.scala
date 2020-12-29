package fr.`override`.linkit.api.utils

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object AsyncExecutionContext {

    private val queue: BlockingDeque[Runnable] = new LinkedBlockingDeque()

    private val worker = new Thread(new ThreadGroup("Relay Execution Worker"), () => {
        while (true) try {
            val action = queue.takeLast()
            action.run()
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
    })

    worker.setName("Relay Async Execution Worker")
    worker.start()

    implicit val context: ExecutionContext = new ExecutionContext() {
        override def execute(runnable: Runnable): Unit = {
            queue.add(runnable)
        }

        override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
    }
}
