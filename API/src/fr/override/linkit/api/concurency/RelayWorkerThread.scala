package fr.`override`.linkit.api.concurency

import java.util.concurrent.{Executors, ThreadFactory}

import fr.`override`.linkit.api.concurency.RelayWorkerThread.factory

class RelayWorkerThread() extends AutoCloseable {

    private val executor = Executors.newFixedThreadPool(3, factory)
    private var closed = false

    def runLater(action: Unit => Unit): Unit = {
        executor.submit((() => action(null)): Runnable)
    }

    override def close(): Unit = {
        closed = true
        executor.shutdown()
    }

}

object RelayWorkerThread {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Relay Worker")
    val factory: ThreadFactory = new Thread(workerThreadGroup, _, "Relay Worker Thread")

    def checkCurrentIsWorker(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalStateException("This action must be performed in a Packet Worker thread !")
    }

    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalStateException("This action must not be performed in a Packet Worker thread !")
    }

    def currentThread(): Option[RelayWorkerThread] = {
        Thread.currentThread() match {
            case worker: RelayWorkerThread => Some(worker)
            case _ => None
        }
    }

    def isCurrentWorkerThread: Boolean = {
        currentThread().isDefined
    }

    def safeLock(anyRef: AnyRef, timeout: Long = 0): Unit = {
        checkCurrentIsNotWorker()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }
}
