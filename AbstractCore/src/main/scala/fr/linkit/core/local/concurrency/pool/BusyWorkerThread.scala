package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.EntertainedThread
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.{currentTaskId, workerThreadGroup}

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

/**
 * The representation of a java thread, extending from [[Thread]].
 * This class contains information that need to be stored into a specific thread class.
 * */
private[concurrency] final class BusyWorkerThread private[concurrency](target: Runnable,
                                                                       override val owner: BusyWorkerPool,
                                                                       id: Int)
        extends Thread(workerThreadGroup, target, s"${owner.name}'s Thread#${id}") with EntertainedThread {

    private val alternatives = mutable.ListBuffer.empty[() => Unit]

    private                        var continueWorkflow          : Boolean = true
    private[concurrency]           var isParkingForRecursiveTask0: Boolean = false
    private[concurrency]           var taskRecursionDepthCount   : Int     = 0
    @volatile private[concurrency] var currentTaskID             : Int     = 0

    def submitAlternative(check: => Boolean, retake: () => Unit): Unit = {
        val alternative = () => if (!check) {
            alternatives -= alternative
            retake
        }
        alternatives += alternative
    }

    /**
     * Will execute awaitAction: T, then execute workflow(T) or retake(T) if the thread should retake this task.
     * all alternatives will be executed before executing awaitAction.
     * @param parkAction the action that will make the thread wait.
     * @param continueWait a condition checked before any execution could take a time to finalize.
     * @param workflow the action to perform after the awaitAction accomplished.
     * @param retake the action to perform if the thread must retake this task.
     * */
    private[concurrency] def awaitTaskSubmissionThen[T](parkAction: => T, continueWait: => Boolean)(workflow: T => Unit)(retake: => Unit): Unit = {
        AppLogger.error(s"${currentTaskId} <> WAITING FOR RECURSIVE TASK...")

        submitAlternative(continueWait, () => retake)

        for (i <- 0 to alternatives.size) {
            if (!continueWait)
                return
            val alternative = alternatives(i)
            alternative.apply()
        }

        isParkingForRecursiveTask0 = true
        val t = parkAction
        AppLogger.error(s"${currentTaskId} <> This thread has been unparked.")
        isParkingForRecursiveTask0 = false
        if (!continueWorkflow || !continueWait) {
            continueWorkflow = true
            return
        }
        workflow(t)
    }

    private[concurrency] def resumeTaskExecution(continueWorkflow: Boolean = true): Unit = {
        this.continueWorkflow = continueWorkflow
        LockSupport.unpark(this)
    }

    override def isWaitingForRecursiveTask: Boolean = isParkingForRecursiveTask0

    override def taskRecursionDepth: Int = taskRecursionDepthCount

}
