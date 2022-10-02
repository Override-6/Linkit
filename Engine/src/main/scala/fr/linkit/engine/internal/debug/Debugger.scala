package fr.linkit.engine.internal.debug

import fr.linkit.api.internal.concurrency.Worker
import fr.linkit.api.internal.system.log.AppLoggers
import org.apache.logging.log4j.Level

import java.io.PrintStream
import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

object Debugger {

    private val threadStates = mutable.HashMap.empty[Thread, ThreadWorkStack]

    def push(action: => Action): Unit = {
        val act = action
        currentStack.push(act)
        act match {
            case _: RequestAction => DeadlockWatchdog.notifyNewRequestPending()
            case _                =>
        }
    }

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


    def dumpTraffic(out: PrintStream = System.out): Unit = {

    }

    private object DeadlockWatchdog extends Thread("Watchdog thread") {
        setDaemon(true)

        start()

        private var lastRequestPending: Long = 0


        override def run(): Unit = {
            while (true) {
                LockSupport.park()
                this.synchronized {
                    val temp = lastRequestPending
                    this.wait(5000) //waiting for 5 seconds
                    def allThreadsAreWaiting = {
                        threadStates.filterInPlace((t, _) => t.isAlive).keys.forall { thread =>
                            val state = thread.getState
                            state == Thread.State.BLOCKED || state == Thread.State.WAITING
                        }
                    }
                    if (temp == lastRequestPending && allThreadsAreWaiting) {
                        AppLoggers.Watchdog.warn("It's looks like the application is facing a deadlock !")
                        AppLoggers.Watchdog.warn("------------------------------------------------------")

                        val watchdogPrintStream = new PrintStream(new LoggerOutputStream(AppLoggers.Watchdog, Level.WARN))
                        watchdogPrintStream.println("Worker dump: ")
                        Debugger.dumpWorkers(watchdogPrintStream)
                        watchdogPrintStream.println("Traffic dump: ")
                        Debugger.dumpTraffic(watchdogPrintStream)
                    }
                }
            }
        }

        def notifyNewRequestPending(): Unit = {
            lastRequestPending = System.currentTimeMillis()
            LockSupport.unpark(this)
        }

    }


}
