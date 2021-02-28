package fr.`override`.linkit.api.concurrency

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool.{RelayThread, checkCurrentIsWorker}
import fr.`override`.linkit.api.exception.IllegalThreadException

import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory}
import scala.util.control.NonFatal

class RelayWorkerThreadPool(val prefix: String, val nThreads: Int) extends AutoCloseable {

    private val factory: ThreadFactory = new RelayThread(_, prefix, this)
    private val executor = Executors.newFixedThreadPool(nThreads, factory)

    private val workQueue = extractWorkQueue() //The pool tasks to execute
    private var closed = false
    private val workersLocks = new WorkersLock
    @volatile private var activeThreads = 0

    override def close(): Unit = {
        closed = true
        executor.shutdownNow()
    }

    def runLater(action: => Unit): Unit = {
        if (!closed) {
            //println(s"Submitted action from thread $currentThread, active threads: $activeThreads")
            var runnable: Runnable = null
            runnable = () => {
                activeThreads += 1
                //println(s"Action taken by thread $currentThread")
                try {
                    action
                } catch {
                    case NonFatal(e) => e.printStackTrace()
                }
                activeThreads -= 1
                // println(s"Action terminated by thread $currentThread, $activeThreads are currently running.")
            }
            executor.submit(runnable)
            //if there is one busy thread that is waiting for a new task to be performed, it would instantly execute the current task.
            workersLocks.notifyOneBusyThread()
        }
    }

    def busyThreads: Int = activeThreads

    def keepBusyWhile(condition: => Boolean): Unit = {
        checkCurrentIsWorker()

        while (!workQueue.isEmpty && condition) {
            val task = workQueue.poll()
            if (task != null) {
                currentWorker.currentProvidingDepth += 1
                task.run()
                currentWorker.currentProvidingDepth -= 1
            }

        }
    }

    def keepBusyWhileThenWait(lock: AnyRef, condition: => Boolean): Unit = {
        keepBusyWhile(condition)

        if (condition) { //we may still need to be busy
            workersLocks.addWorkLock(lock)
            lock.synchronized {
                if (condition) { // because of the synchronisation block, the condition value may change
                    lock.wait()
                }
            }
            workersLocks.releaseWorkLock()
        }
    }

    def keepBusyWhileOrWait(lock: AnyRef, condition: => Boolean): Unit = {
        keepBusyWhile(condition)
        lock.synchronized {
            if (condition) {
                lock.wait()
            }
        }
        if (condition) //We still need to be busy
            keepBusyWhileOrWait(lock, condition)
    }

    //FIXME StackOverflowError if workQueue is empty
    def keepBusy(millis: Long): Unit = {
        checkCurrentIsWorker()

        var totalProvided: Long = 0
        while (!workQueue.isEmpty && totalProvided <= millis) {
            val t0 = now()
            workQueue.take().run()
            val t1 = now()
            totalProvided += (t1 - t0)
        }
        val toWait = millis - totalProvided
        if (toWait > 0) {
            val waited = timedWait(getClass, toWait)
            if (waited < toWait)
                keepBusy(millis)
        }
    }

    def newBusyQueue[A]: BlockingQueue[A] = {
        new BusyBlockingQueue[A](this)
    }

    @relayWorkerExecution
    def currentProvidingDepth: Int = {
        checkCurrentIsWorker("This action is only permitted to relay threads")
        currentWorker.currentProvidingDepth
    }

    private def currentWorker: RelayThread = {
        currentThread.asInstanceOf[RelayThread]
    }

    private def extractWorkQueue(): BlockingQueue[Runnable] = {
        val clazz = executor.getClass
        val field = clazz.getDeclaredField("workQueue")
        field.setAccessible(true)
        field.get(executor).asInstanceOf[BlockingQueue[Runnable]]
    }
}

object RelayWorkerThreadPool {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Relay Worker")
    private var activeCount = 1

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread does not extends from [[RelayThread]], this would mean that,
     * we are not running into a thread that is owned by the Relay concurrency system. Therefore, the action
     * may be performed as a synchronized action.
     *
     * @param action the action to perform
     * */
    def smartRunLater(action: => Unit): Unit = {
        ifCurrentWorkerOrElse(_.runLater(action), action)
    }

    def checkCurrentIsWorker(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed in a Packet Worker thread !")
    }

    def checkCurrentIsWorker(msg: String): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must be performed in a Packet Worker thread ! ($msg)")
    }

    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed in a Packet Worker thread !")
    }

    def checkCurrentIsNotWorker(msg: String): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException(s"This action must not be performed in a Packet Worker thread ! ($msg)")
    }

    def isCurrentWorkerThread: Boolean = {
        currentThread.getThreadGroup == workerThreadGroup
    }

    def smartKeepBusy(asLongAs: => Boolean): Unit = {
        ifCurrentWorkerOrElse(_.keepBusyWhile(asLongAs), ())
    }

    def smartKeepBusy(lock: AnyRef, asLongAs: => Boolean): Unit = {
        ifCurrentWorkerOrElse(_.keepBusyWhileOrWait(lock, asLongAs), if (asLongAs) lock.synchronized(lock.wait()))
    }

    def smartKeepBusy(lock: AnyRef, minTimeOut: Long): Unit = {
        ifCurrentWorkerOrElse(_.keepBusy(minTimeOut), lock.synchronized(lock.wait(minTimeOut)))
    }

    def smartWait(lock: AnyRef, millis: Int): Unit = {
        ifCurrentWorkerOrElse(_.keepBusy(millis), lock.synchronized(lock.wait(millis)))
    }

    def ifCurrentWorkerOrElse[A](ifCurrent: RelayWorkerThreadPool => A, orElse: => A): A = {
        val pool = currentPool()

        if (pool.isDefined) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
    }

    def currentPool(): Option[RelayWorkerThreadPool] = {
        currentThread match {
            case worker: RelayThread => Some(worker.owner)
            case _ => None
        }
    }

    private class RelayThread private[RelayWorkerThreadPool](target: Runnable, prefix: String,
                                                             private[RelayWorkerThreadPool] val owner: RelayWorkerThreadPool)
        extends Thread(workerThreadGroup, target, s"$prefix Relay Worker Thread-" + activeCount) {
        activeCount += 1

        var currentProvidingDepth = 0

    }

}