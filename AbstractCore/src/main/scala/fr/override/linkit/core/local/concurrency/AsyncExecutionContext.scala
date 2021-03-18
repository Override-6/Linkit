package fr.`override`.linkit.core.local.concurrency

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object AsyncExecutionContext extends ExecutionContext {
    private val ses = Executors.newFixedThreadPool(5, (r: Runnable) => {
        println("Created new thread for async context !")
        new Thread(SyncExecutionContext.threadGroup, r)
    })

    override def execute(runnable: Runnable): Unit = ses.submit(runnable)

    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
}
