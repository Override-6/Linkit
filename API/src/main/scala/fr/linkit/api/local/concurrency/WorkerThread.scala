package fr.linkit.api.local.concurrency

trait WorkerThread extends Thread {
    val pool: Procrastinator

    def taskRecursionDepth: Int

    def isWaitingForRecursiveTask: Boolean

    def currentTaskID: Int
}