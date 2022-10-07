package fr.linkit.engine.internal.concurrency

import fr.linkit.api.internal.concurrency.{Procrastinator, Worker, WorkerPool}
import fr.linkit.engine.internal.concurrency.VirtualProcrastinator.workers

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class VirtualProcrastinator(val name: String) extends WorkerPool {

    private val taskCounter = new AtomicInteger()
    private val context     = ExecutionContext.fromExecutorService(Executors.newThreadPerTaskExecutor(makeVThread(_)), _.printStackTrace())

    override def runLater[A](f: => A): Future[A] = Future {
        try f catch {
            case t: Throwable =>
                t.printStackTrace()
                throw t
        }
    }(context)

    private def makeVThread(target: Runnable): Thread = {
        val taskID = taskCounter.incrementAndGet()
        val thread = Thread.ofVirtual().name(name + "'s task #" + taskID).unstarted(target)
        val worker = new VirtualWorker(this, thread, taskID)
        workers.put(thread, worker)
        thread
    }
}

object VirtualProcrastinator extends Procrastinator.Supplier {

    private val workers = mutable.HashMap.empty[Thread, VirtualWorker]

    override def apply(name: String): VirtualProcrastinator = new VirtualProcrastinator(name)

    override def current: Option[Procrastinator] = workers.get(Thread.currentThread()).map(_.pool)

    override def currentWorker: Option[Worker] = workers.get(Thread.currentThread())
}
