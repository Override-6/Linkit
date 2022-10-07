package fr.linkit.engine.internal.debug

import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.debug.Debugger.threadStates
import org.apache.logging.log4j.Level

import java.io.PrintStream
import java.util.concurrent.locks.LockSupport

private[debug] object DeadlockWatchdog extends Thread("Watchdog Thread") {
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

    def startDebuggerSession(): Unit = {
        makeDump(new PrintStream(new LoggerOutputStream(AppLoggers.Watchdog, Level.WARN)))
    }

    private def makeDump(out: PrintStream): Unit = {
        out.println("It's looks like the application is facing a deadlock !")
        out.println("------------------------------------------------------")

        out.println("worker dump: ")
        Debugger.dumpWorkers(out)
        out.println("\ntraffic dump: ")
        Debugger.dumpTraffic(out)
    }

    def notifyNewRequestPending(): Unit = {
        lastRequestPending = System.currentTimeMillis()
        LockSupport.unpark(this)
    }

}