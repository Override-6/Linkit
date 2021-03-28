package fr.linkit.core.local.concurrency

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.local.system.AppLogger

import scala.collection.mutable.ListBuffer

class WorkerEntertainer(pool: BusyWorkerPool) {

    type WorkerThread = BusyWorkerPool#WorkerThread
    private val entertainedThreads = new ListBuffer[WorkerThread]()

    @workerExecution
    def amuseCurrentThread(): Unit = {
        amuseCurrentThreadWhile(true)
    }

    @workerExecution
    def amuseCurrentThreadWhile(condition: => Boolean): Unit = {
        pool.ensureCurrentThreadOwned("Threads cannot play with strangers.")
        AppLogger.error("Amusing current thread...")
        val currentWorker = BusyWorkerPool.currentWorkerThread
        entertainedThreads += currentWorker
        BusyWorkerPool.executeRemainingTasksWhile(condition)
        entertainedThreads -= currentWorker
    }

    @workerExecution
    def stopFirstNThreadAmusement(n: Int): Unit = {
        stopThreadsAmusement( _.take(n))
    }

    @workerExecution
    def stopFirstThreadAmusement(): Unit = {
        stopFirstNThreadAmusement(1)
    }

    @workerExecution
    def stopLastNThreadAmusement(n: Int): Unit = {
        stopThreadsAmusement( _.takeRight(n))
    }

    @workerExecution
    def stopLastThreadAmusement(): Unit = {
        stopLastNThreadAmusement(1)
    }

    @workerExecution
    private def stopThreadsAmusement(action: ListBuffer[WorkerThread] => Iterable[WorkerThread]): Unit = {
        pool.ensureCurrentThreadOwned("Threads cannot play with strangers.")

        val unamused = action(entertainedThreads)

        AppLogger.error("unamused = " + unamused)
        AppLogger.error("entertainedThreads = " + entertainedThreads)

        if (unamused.isEmpty || entertainedThreads.isEmpty)
            return //Instruction below could throw NoSuchElementException when removing unamused list to entertainedThreads.

        for (thread <- unamused) {
            BusyWorkerPool.stopExecuteRemainingTasks(thread)
        }
        entertainedThreads --= unamused
    }

}
