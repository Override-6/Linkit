package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable.ListBuffer

class WorkerEntertainer(pool: BusyWorkerPool) {

    private val entertainedThreads = new ListBuffer[BusyWorkerThread]()

    def this() {
        this(BusyWorkerPool.ensureCurrentIsWorker())
    }

    @workerExecution
    def amuseCurrentThread(): Unit = {
        amuseCurrentThreadWhile(true)
    }

    @workerExecution
    def amuseCurrentThreadWhile(condition: => Boolean): Unit = {
        pool.ensureCurrentThreadOwned("Current thread ")
        AppLogger.error(s"$currentTasksId <> Amusing current thread...")
        val currentWorker = BusyWorkerPool.currentWorker
        entertainedThreads += currentWorker
        BusyWorkerPool.executeRemainingTasksWhileThen(condition)
        entertainedThreads -= currentWorker
    }

    @workerExecution
    def stopFirstNThreadAmusement(n: Int): Unit = {
        stopThreadsAmusement(_.take(n))
    }

    @workerExecution
    def stopFirstThreadAmusement(): Unit = {
        stopFirstNThreadAmusement(1)
    }

    @workerExecution
    def stopLastNThreadAmusement(n: Int): Unit = {
        stopThreadsAmusement(_.takeRight(n))
    }

    @workerExecution
    def stopLastThreadAmusement(): Unit = {
        stopLastNThreadAmusement(1)
    }

    @workerExecution
    def stopThreadAmusement(thread: BusyWorkerThread): Unit = {
        if (!entertainedThreads.contains(thread))
            throw new NoSuchElementException(s"Provided thread is not entertained ! (${thread.getName})")
        BusyWorkerPool.stopWaitRemainingTasks(thread)
    }

    @workerExecution
    private def stopThreadsAmusement(action: ListBuffer[BusyWorkerThread] => Iterable[BusyWorkerThread]): Unit = {

        val unamused = action(entertainedThreads)

        AppLogger.error(s"$currentTasksId <> unamused = " + unamused)
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)

        if (unamused.isEmpty || entertainedThreads.isEmpty)
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.

        for (thread <- unamused) {
            BusyWorkerPool.stopWaitRemainingTasks(thread)
        }
        entertainedThreads --= unamused
    }

}
