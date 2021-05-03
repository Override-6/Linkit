package fr.linkit.api.local.concurrency

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object WorkerPools {
    val workerThreadGroup: ThreadGroup = new ThreadGroup("Application Worker")

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread is not a relay worker thread, this would mean that
     * we are not running into a thread that is owned by the Relay concurrency system. Therefore, the action
     * may be performed in the current thread
     *
     * @param action the action to perform
     * */
    def runLaterOrHere(action: => Unit): Unit = {
        currentPool.fold(action)(_.runLater(action))
    }

    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def runLaterInCurrentPool(@workerExecution action: => Unit): Unit = {
        val pool = ensureCurrentIsWorker("Could not run request action because current thread does not belong to any worker pool")
        pool.runLater(action)
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[WorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(): WorkerPool = {
        if (!isCurrentThreadWorker)
            throw IllegalThreadException("This action must be performed by a Worker thread !")
        currentPool.get
    }

    /**
     * @throws IllegalThreadException if the current thread is a [[WorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is not an instance of WorkerThread.")
    def ensureCurrentIsWorker(msg: String): WorkerPool = {
        if (!isCurrentThreadWorker)
            throw IllegalThreadException(s"This action must be performed by a Worker thread ! ($msg)")
        currentPool.get
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def ensureCurrentIsNotWorker(): Unit = {
        if (isCurrentThreadWorker)
            throw IllegalThreadException("This action must not be performed by a Worker thread !")
    }

    /**
     * @throws IllegalThreadException if the current thread is not a [[WorkerThread]]
     * @param msg the message to complain with the exception
     * */
    @throws[IllegalThreadException]("If the current thread that executes this method is an instance of WorkerThread.")
    def ensureCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentThreadWorker)
            throw IllegalThreadException(s"This action must not be performed by a Worker thread ! ($msg)")
    }

    /**
     * @return {{{true}}} if and only if the current thread is an instance of [[WorkerThread]]
     * */
    def isCurrentThreadWorker: Boolean = {
        currentThread.isInstanceOf[WorkerThread]
    }

    /**
     * Toggles between two actions if the current thread is an instance of [[WorkerThread]]
     *
     * @param ifCurrent The action to process if the current thread is a relay worker thread.
     *                  The given entry is the current thread pool
     * @param orElse    the action to process if the current thread is not a relay worker thread.
     *
     * */
    def ifCurrentWorkerOrElse[A](ifCurrent: WorkerPool => A, orElse: => A): A = {
        val pool = currentPool

        if (pool.isDefined) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
    }

    /**
     * @return Some if the current thread is a member of a [[BusyWorkerPool]], None instead
     * */
    implicit def currentPool: Option[WorkerPool] = {
        currentThread match {
            case worker: WorkerThread => Some(worker.pool)
            case _                    => None
        }
    }

    implicit def currentExecutionContext: ExecutionContext = {
        currentPool match {
            case Some(pool) => pool
            case None       => global
        }
    }

    @workerExecution
    def currentWorker: WorkerThread = {
        currentThread match {
            case worker: WorkerThread => worker
            case _                    => throw IllegalThreadException("Current thread is not a WorkerThread.")
        }
    }

    @workerExecution
    def currentTask: AsyncTask[_] = {
        currentWorker.getCurrentTask.get
    }

    @workerExecution
    def currentTaskWithController: Option[AsyncTask[_] with AsyncTaskController] = {
        if (!isCurrentThreadWorker)
            return None
        currentWorker.getController.getCurrentTask
    }

    def currentTasksId: String = {
        if (isCurrentThreadWorker)
            currentWorker.prettyPrintPrefix
        else "?"
    }

    private def currentThread: Thread = Thread.currentThread()

}
