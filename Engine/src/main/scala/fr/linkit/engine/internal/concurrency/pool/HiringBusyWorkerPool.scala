package fr.linkit.engine.internal.concurrency.pool

import java.util.concurrent.ThreadFactory

import fr.linkit.engine.internal.concurrency.pool.HiringBusyWorkerPool.HiringTicket

import scala.collection.mutable

class HiringBusyWorkerPool(name: String) extends AbstractWorkerPool(0, name) {

    private val tickets = mutable.Queue.empty[HiringTicket]

    def hireCurrentThread(): Nothing = {
        val ticket = new HiringTicket(Thread.currentThread())
        tickets += ticket
        setThreadCount(threadCount + 1)
        ticket.go()
        //The method will never end
        throw new Error()
    }

    override protected def getThreadFactory: ThreadFactory = { runnable =>
        val ticket       = tickets.dequeue()
        val thread       = ticket.thread
        val workerThread = new HiredWorker(ticket.thread, this)
        workers += workerThread
        ticket.informHired(runnable)
        thread
    }
}

object HiringBusyWorkerPool {

    class HiringTicket(val thread: Thread) {
        private var runnable: Runnable = _

        def go(): Unit = {
            this.wait()
            runnable.run()
        }

        def informHired(runnable: Runnable): Unit = this.synchronized {
            this.runnable = runnable
            this.notify()
        }
    }

}
