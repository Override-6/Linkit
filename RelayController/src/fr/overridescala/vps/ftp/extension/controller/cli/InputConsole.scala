package fr.overridescala.vps.ftp.`extension`.controller.cli

import java.util.concurrent.PriorityBlockingQueue

import scala.annotation.tailrec
import scala.io.StdIn

object InputConsole {

    private val ticketQueue = new PriorityBlockingQueue[InputRequestTicket]()

    def requestNextInput(priority: Int = 0): String = {
        val requestTicket = new InputRequestTicket(priority)
        ticketQueue.add(requestTicket)
        requestTicket.getLine
    }

    def ask(msg: String, possibleResponses: String*): String = {
        println(msg)
        var input = requestNextInput(10)
        if (possibleResponses.isEmpty)
            input
        else {
            while (possibleResponses.forall(_.toLowerCase != input)) {
                println(msg)
                input = requestNextInput(10)
            }
            input
        }
    }

    start()

    private def start(): Unit = {
        val consoleThread = new Thread(() => {
            while (true) {
                val line = StdIn.readLine()
                ticketQueue.take().setLine(line)
            }
        })
        consoleThread.setName("Console Inputs Queue")
        consoleThread.start()
    }

    class InputRequestTicket(private val priority: Int) extends Comparable[InputRequestTicket] {
        @volatile private var line: String = _
        private val threadOwner = Thread.currentThread()

        def getLine: String = {
            if (line == null)
                synchronized {
                    wait()
                }
            line
        }

        override def toString: String = s"owner : $threadOwner"

        def setLine(line: String): Unit = {
            this.line = line
            synchronized {
                notify()
            }
        }

        override def compareTo(o: InputRequestTicket): Int =
            o.priority - priority
    }

}
