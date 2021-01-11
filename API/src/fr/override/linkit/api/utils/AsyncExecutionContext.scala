package fr.`override`.linkit.api.utils

import java.util.concurrent.{Executors, TimeUnit}

import scala.concurrent.ExecutionContext

object AsyncExecutionContext extends ExecutionContext {
    private val ses = Executors.newScheduledThreadPool(5, (r: Runnable) => new Thread(SyncExecutionContext.threadGroup, r))

    override def execute(runnable: Runnable): Unit = ses.schedule(runnable, 0, TimeUnit.MILLISECONDS)

    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
}
