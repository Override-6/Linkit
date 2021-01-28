package fr.`override`.linkit.api.concurrency

import java.util.concurrent.{BlockingDeque, BlockingQueue, Executors, LinkedBlockingDeque, ThreadFactory}

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool.{WorkerThread, checkCurrentIsWorker}

import scala.util.control.NonFatal

class RelayWorkerThreadPool() extends AutoCloseable {

    val factory: ThreadFactory = new WorkerThread(_, this)
    private val executor = Executors.newFixedThreadPool(3, factory)

    //The different tasks to make
    private val workQueue = extractWorkQueue()
    private var closed = false
    private val lock: AnyRef = new Object()

    def runLater(action: => Unit): Unit = {
        runLater(_ => action)
    }

    def runLater(action: Unit => Unit): Unit = {
        if (!closed) {
            executor.submit((() => {
                try {
                    action(null)
                } catch {
                    case NonFatal(e) => e.printStackTrace()
                }
            }): Runnable)
            lock.synchronized {
                //notifies one thread that was provided with other tasks, and waited for the queue to be not empty
                lock.notify()
            }
        }
    }

    def provideWhile(check: => Boolean): Unit = {
        checkCurrentIsWorker()

        while (!workQueue.isEmpty && check) {
            workQueue.take().run()
        }
    }

    def provideWhileThenWait(lock: AnyRef, check: => Boolean): Unit = {
        provideWhile(check)

        if (check) { //we may still need to provide
            lock.synchronized {
                lock.wait()
            }
        }
    }
    def provideWhileThenWait(check: => Boolean): Unit = {
        provideWhileThenWait(this.lock, check)
        if (check) { //we may still need to provide
            provideWhileThenWait(check)
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
            val waited = timedWait(lock, toWait)
            if (waited < toWait)
                provide(millis)
        }
    }

    override def close(): Unit = {
        closed = true
        executor.shutdownNow()
    }

    def extractWorkQueue(): BlockingQueue[Runnable] = {
        val clazz = executor.getClass
        val field = clazz.getDeclaredField("workQueue")
        field.setAccessible(true)
        field.get(executor).asInstanceOf[BlockingQueue[Runnable]]
    }

    def createProvidedQueue[A]: BlockingDeque[A] = {
        val queue = new LinkedBlockingDeque[A]()
        val clazz = queue.getClass
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
     * @return true if the action has been executed into the pool
     * */
    def smartRun(action: => Unit): Boolean = {
        def makeAction(): Unit = action

        ifCurrentOrElse(_.runLater(makeAction()), makeAction())
    }

    def checkCurrentIsWorker(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalStateException("This action must be performed in a Packet Worker thread !")
    }

    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalStateException("This action must not be performed in a Packet Worker thread !")
    }

    def smartWait(lock: AnyRef, asLongAs: => Boolean): Boolean = {
        ifCurrentOrElse(_.provideWhileThenWait(lock, asLongAs), lock.synchronized(lock.wait()))
    }

    def isCurrentWorkerThread: Boolean = {
        Thread.currentThread().getThreadGroup == workerThreadGroup
    }

    def smartWait(asLongAs: => Boolean): Boolean = {
        ifCurrentOrElse(_.provideWhile(asLongAs), ())
    }

    def smartWait(lock: AnyRef, minTimeOut: Long): Boolean = {
        ifCurrentOrElse(_.provide(minTimeOut), lock.synchronized(lock.wait(minTimeOut)))
    }

    def ifCurrentOrElse(ifCurrent: RelayWorkerThreadPool => Unit, orElse: => Unit): Boolean = {
        val pool = RelayWorkerThreadPool.currentThreadPool()
        val isKnown = pool.isDefined

        if (isKnown) {
            ifCurrent(pool.get)
        } else {
            orElse
        }
        isKnown
    }

    def currentThreadPool(): Option[RelayWorkerThreadPool] = {
        Thread.currentThread() match {
            case worker: WorkerThread => Some(worker.owner)
            case _ => None
        }
    }

    def smartWait(lock: AnyRef, millis: Int): Unit = {
        ifCurrentOrElse(_.provide(millis), lock.synchronized(lock.wait(millis)))
    }

    class WorkerThread private[RelayWorkerThreadPool](target: Runnable,
                                                      private[RelayWorkerThreadPool] val owner: RelayWorkerThreadPool)
            extends Thread(workerThreadGroup, target, "Relay Worker Thread-" + activeCount) {
        activeCount += 1
    }
}