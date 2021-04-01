package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.{currentTasksId, currentWorker}

import scala.collection.mutable.ListBuffer

class WorkerEntertainer(pool: BusyWorkerPool) {

    private val entertainedThreads = new ListBuffer[BusyWorkerThread]()

    def this() {
        this(BusyWorkerPool.ensureCurrentIsWorker())
    }

    @workerExecution
    def amuseCurrentThread(): Unit = {
        entertainedThreads += currentWorker
        pool.executeRemainingTasksOrWait()
    }

    @workerExecution
    def amuseCurrentThreadFor(millis: Long): Unit = {
        entertainedThreads += currentWorker
        pool.timedExecuteRemainingTasks(millis)
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
            throw new NoSuchElementException(s"Provided thread is not entertained by this entertainer ! (${thread.getName})")
        BusyWorkerPool.stopWaitRemainingTasks(thread)
    }

    @workerExecution
    private def stopThreadsAmusement(action: ListBuffer[BusyWorkerThread] => Iterable[BusyWorkerThread]): Unit = {

        val unamused = action(entertainedThreads)

        AppLogger.error(s"$currentTasksId <> unamused = " + unamused)
        AppLogger.error(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)

        if (unamused.isEmpty || entertainedThreads.isEmpty) {
            AppLogger.error("THREADS WERE EMPTY !")
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.
        }

        for (thread <- unamused) {
            BusyWorkerPool.stopWaitRemainingTasks(thread)
        }
        entertainedThreads --= unamused
    }

}
