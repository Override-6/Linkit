package fr.linkit.api.local.concurrency

trait WorkerController[W <: WorkerThread] {

    @workerExecution
    def waitTask(): Unit

    @workerExecution
    def waitTask(millis: Long): Unit

    @workerExecution
    def notifyNThreads(n: Int): Unit

    @workerExecution
    def notifyAnyThread(): Unit

    @workerExecution
    def notifyThreadsTasks(taskIds: Int*): Unit

    @workerExecution
    def notifyWorkerTask(thread: W, taskID: Int): Unit
}
