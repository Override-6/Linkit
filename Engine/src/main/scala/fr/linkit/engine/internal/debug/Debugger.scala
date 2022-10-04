package fr.linkit.engine.internal.debug

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.internal.concurrency.Worker
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import org.apache.logging.log4j.Level

import java.io.PrintStream
import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

object Debugger {

    private val threadStates = mutable.HashMap.empty[Thread, ThreadWorkStack]

    private def callerTrace = Thread.currentThread().getStackTrace.drop(3).head

    private val connections = mutable.Set.empty[ConnectionContext]

    //FIXME find a better way
    private[linkit] def registerConnection(connectionContext: ConnectionContext): Unit = connections += connectionContext

    def push(action: => Action): Unit = {
        val act = action
        //AppLoggers.Watchdog.debug("push - " + callerTrace + s"($act)")
        currentStack.push(act)
        act match {
            case _: RequestAction => DeadlockWatchdog.notifyNewRequestPending()
            case _                =>
        }
    }

    def pop(): Action = {
        val popped = currentStack.pop()
        //AppLoggers.Watchdog.debug("pop - " + callerTrace + " (" + popped + ")")
        popped
    }

    private def currentStack: ThreadWorkStack = {
        val currentThread = Thread.currentThread()
        threadStates.getOrElseUpdate(currentThread, new ThreadWorkStack(currentThread))
    }

    def dumpWorkers(out: PrintStream = System.out): Unit = {
        val (workers, others) = threadStates.keys.partitionMap {
            case w: Worker => Left(w)
            case o         => Right(o)
        }
        workers.groupBy(_.pool).foreach { case (pool, poolWorkers) =>
            out.println(s"worker pool '${pool.name}': ")
            poolWorkers.toArray.sortBy(_.getName).foreach(threadStates(_).printStack(out))
        }
        if (others.nonEmpty) {
            out.println("other threads:")
            others.toArray.sortBy(_.getName).foreach(threadStates(_).printStack(out))
        }
    }

    def dumpTraffic(out: PrintStream = System.out): Unit = connections.foreach(dumpConnectionTraffic(_, out))

    def dumpConnectionTraffic(connection: ConnectionContext, out: PrintStream = System.out): Unit = {
        val connectionServerID = connection.network.serverIdentifier
        connection.traffic match {
            case traffic: AbstractPacketTraffic =>
                out.println(s"Dumping connection '$connectionServerID' traffic:")
                traffic.dump(out)
            case _                              =>
                out.println(s"Could not dump connection '$connectionServerID' traffic.")
        }
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

                    def anyThreadIsWaiting: Boolean = {
                        !threadStates.filterInPlace((t, st) => t.isAlive || !st.isEmpty)
                                     .keys.exists(_.getState == Thread.State.RUNNABLE)
                    }

                    if (temp == lastRequestPending && anyThreadIsWaiting) {
                        AppLoggers.Watchdog.warn("It's looks like the application is facing a deadlock !")
                        AppLoggers.Watchdog.warn("------------------------------------------------------")

                        val watchdogPrintStream = new PrintStream(new LoggerOutputStream(AppLoggers.Watchdog, Level.WARN))
                        watchdogPrintStream.println("worker dump: ")
                        Debugger.dumpWorkers(watchdogPrintStream)
                        watchdogPrintStream.println("\nTraffic dump: ")
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
