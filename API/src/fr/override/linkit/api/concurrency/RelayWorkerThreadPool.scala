package fr.`override`.linkit.api.concurrency

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool.{RelayThread, checkCurrentIsWorker}
import fr.`override`.linkit.api.exception.IllegalThreadException

import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory}
import scala.util.control.NonFatal

class RelayWorkerThreadPool(val prefix: String, val nThreads: Int) extends AutoCloseable {

    val factory: ThreadFactory = new RelayThread(_, prefix, this)
    private val executor = Executors.newFixedThreadPool(nThreads, factory)

    private val workQueue = extractWorkQueue() //The pool tasks to execute
    private var closed = false
    private val providerLocks = new ProvidersLock
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
            //if there is one provided thread that is waiting for a new task to be performed, it would instantly execute the current task.
            providerLocks.notifyOneProvider()
        }
    }

    def workingThreads: Int = activeThreads

    def provideWhile(condition: => Boolean): Unit = {
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

    def provideAllWhileThenWait(lock: AnyRef, condition: => Boolean): Unit = {
        provideWhile(condition)

        if (condition) { //we may still need to provide
            providerLocks.addProvidingLock(lock)
            while (condition) {
                lock.synchronized {
                    if (condition) { // because of the synchronisation block, the condition value may change
                        lock.wait()
                    }
                }
                provideWhile(condition)
            }
            providerLocks.removeProvidingLock()
        }
    }

    def provideWhileOrWait(lock: AnyRef, condition: => Boolean): Unit = {
        provideWhile(condition)
        lock.synchronized {
            if (condition) {
                lock.wait()
            }
        }
        if (condition) //We still need to provide
            provideWhileOrWait(lock, condition)
    }

    def provide(millis: Long): Unit = {
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
                provide(millis)
        }
    }

    private def extractWorkQueue(): BlockingQueue[Runnable] = {
        val clazz = executor.getClass
        val field = clazz.getDeclaredField("workQueue")
        field.setAccessible(true)
        field.get(executor).asInstanceOf[BlockingQueue[Runnable]]
    }

    def newProvidedQueue[A]: BlockingQueue[A] = {
        new ProvidedBlockingQueue[A](this)
    }

    @relayWorkerExecution
    def currentProvidingDepth: Int = {
        checkCurrentIsWorker("This action is only permitted to relay threads")
        currentWorker.currentProvidingDepth
    }

    private def currentWorker: RelayThread = {
        currentThread.asInstanceOf[RelayThread]
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
        ifCurrentOrElse(_.runLater(action), action)
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

    def smartProvide(asLongAs: => Boolean): Unit = {
        ifCurrentOrElse(_.provideWhile(asLongAs), ())
    }

    def smartProvide(lock: AnyRef, asLongAs: => Boolean): Unit = {
        ifCurrentOrElse(_.provideAllWhileThenWait(lock, asLongAs), if (asLongAs) lock.synchronized(lock.wait()))
    }

    def smartProvide(lock: AnyRef, minTimeOut: Long): Unit = {
        ifCurrentOrElse(_.provide(minTimeOut), lock.synchronized(lock.wait(minTimeOut)))
    }

    def ifCurrentOrElse[A](ifCurrent: RelayWorkerThreadPool => A, orElse: => A): A = {
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

    def smartWait(lock: AnyRef, millis: Int): Unit = {
        ifCurrentOrElse(_.provide(millis), lock.synchronized(lock.wait(millis)))
    }

    private class RelayThread private[RelayWorkerThreadPool](target: Runnable, prefix: String,
                                                             private[RelayWorkerThreadPool] val owner: RelayWorkerThreadPool)
            extends Thread(workerThreadGroup, target, s"$prefix Relay Worker Thread-" + activeCount) {
        activeCount += 1

        var currentProvidingDepth = 0

    }

}