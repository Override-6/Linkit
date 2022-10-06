package fr.linkit.engine.internal.debug

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.internal.concurrency.Worker
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic

import java.io.PrintStream
import java.util.Scanner
import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

class Debugger(connection: ConnectionContext) {

}

object Debugger {

    private val threadStates = mutable.HashMap.empty[Thread, ThreadWorkStack]

    //private def callerTrace = Thread.currentThread().getStackTrace.drop(3).head

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


    private object DeadlockWatchdog extends Thread("Watchdog Thread") {
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
                        startDebuggerSession()
                    }
                }
            }
        }

        def newTerminal(): Process = {
            //TODO Support macos and windows
            Runtime.getRuntime.exec("/usr/bin/x-terminal-emulator --disable-factory -e cat")
        }

        def startDebuggerSession(): Unit = {
            val prompt = newTerminal()
            val out    = new PrintStream(prompt.getOutputStream)
            val in     = new Scanner(prompt.getInputStream)
            makeDump(out)
            var line = in.nextLine().trim
            while (line != "exit") {
                out.println(s"you entered: $line")
                line = in.nextLine()
            }
        }

        private def makeDump(out: PrintStream): Unit = {
            out.println("It's looks like the application is facing a deadlock !")
            out.println("------------------------------------------------------")

            out.println("worker dump: ")
            Debugger.dumpWorkers(out)
            out.println("\nTraffic dump: ")
            Debugger.dumpTraffic(out)
        }

        def notifyNewRequestPending(): Unit = {
            lastRequestPending = System.currentTimeMillis()
            LockSupport.unpark(this)
        }

    }

}
