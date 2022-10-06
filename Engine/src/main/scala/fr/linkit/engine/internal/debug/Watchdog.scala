package fr.linkit.engine.internal.debug

import fr.linkit.api.internal.system.log.AppLoggers
import org.apache.logging.log4j.Level

import java.io.PrintStream
import java.util.concurrent.locks.LockSupport

class Watchdog(debugger: Debugger) extends Thread("Watchdog thread") {
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