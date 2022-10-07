package fr.linkit.api.internal.concurrency

trait Worker {
    val thread: Thread

    val pool: WorkerPool

    val taskID: Int
}
