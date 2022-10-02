package fr.linkit.engine.internal.debug

import fr.linkit.api.internal.concurrency.Worker

import java.io.PrintStream
import scala.collection.mutable

object Debugger {

    private val threadStates = mutable.HashMap.empty[Thread, ThreadWorkStack]

    def push(action: => Action): Unit = currentStack.push(action)

    def pop(): Unit = currentStack.pop()

    private def currentStack: ThreadWorkStack = {
        val currentThread = Thread.currentThread()
        threadStates.getOrElseUpdate(currentThread, new ThreadWorkStack(currentThread))
    }

    def dumpWorkers(out: PrintStream = System.out): Unit = {
        val (workers, others) = threadStates.keys.partitionMap {
            case w: Worker => Left(w)
            case o         => Right(o)
        }
        workers.groupBy(_.pool).foreach { case (pool, workers) =>
            out.println(s"worker pool '${pool.name}': ")
            workers.foreach(threadStates(_).printStack(out))
        }
        out.println("other threads:")
        others.foreach(threadStates(_).printStack(out))
    }


    def dumpTraffic(): Unit = {

    }

}
