package fr.linkit.api.local.concurrency

trait EntertainedThread extends Thread {
    val owner: Procrastinator

    def taskRecursionDepth: Int

    def isWaitingForRecursiveTask: Boolean
}