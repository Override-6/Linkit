package fr.`override`.linkit.api.concurrency

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool.{WorkerThread, checkCurrentIsWorker}
import fr.`override`.linkit.api.exception.IllegalThreadException

import java.util.concurrent.{BlockingQueue, Executors, ThreadFactory}
import scala.util.control.NonFatal

class RelayWorkerThreadPool(val prefix: String, val nThreads: Int) extends AutoCloseable {

    val factory: ThreadFactory = new WorkerThread(_, prefix, this)
    private val executor = Executors.newFixedThreadPool(nThreads, factory)

    private val workQueue = extractWorkQueue() //The different tasks to execute
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

    def provideWhile(check: => Boolean): Unit = {
        checkCurrentIsWorker()

        while (!workQueue.isEmpty && check) {
            val task = workQueue.poll()
            if (task != null)
                task.run()
        }
    }

    def provideAllWhileThenWait(lock: AnyRef, check: => Boolean): Unit = {
        provideWhile(check)

        if (check) { //we may still need to provide
            providerLocks.addProvidingLock(lock)
            while (check) {
                lock.synchronized {
                    if (workQueue.isEmpty && check) { // because of the synchronisation block, the check value may change
                        lock.wait()
                    }
                }
                provideWhile(check)
            }
            providerLocks.removeProvidingLock()
        }
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
}

object RelayWorkerThreadPool {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Relay Worker")
    private var activeCount = 1

    /**
     * This method may execute the given action into the current thread pool.
     * If the current execution thread does not extends from [[WorkerThread]], this would mean that,
     * we are not running into a thread that is owned by the Relay concurrency system. Therefore, the action
     * may be performed as a synchronized action.
     *
     * @param action the action to perform
     * */
    def smartRun(action: => Unit): Unit = {
        def makeAction(): Unit = action

        ifCurrentOrElse(_.runLater(makeAction()), makeAction())
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
        val pool = currentThreadPool()

        if (pool.isDefined) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
    }

    def currentThreadPool(): Option[RelayWorkerThreadPool] = {
        currentThread match {
            case worker: WorkerThread => Some(worker.owner)
            case _ => None
        }
    }

    def smartWait(lock: AnyRef, millis: Int): Unit = {
        ifCurrentOrElse(_.provide(millis), lock.synchronized(lock.wait(millis)))
    }

    class WorkerThread private[RelayWorkerThreadPool](target: Runnable, prefix: String,
                                                      private[RelayWorkerThreadPool] val owner: RelayWorkerThreadPool)
            extends Thread(workerThreadGroup, target, s"$prefix Relay Worker Thread-" + activeCount) {
        activeCount += 1
    }

}