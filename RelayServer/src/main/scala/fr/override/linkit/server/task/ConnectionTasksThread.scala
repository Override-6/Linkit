package fr.`override`.linkit.server.task

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.`override`.linkit.skull.connection.network.RemoteConsole
import .PacketInjection
import fr.`override`.linkit.skull.internal.system.{CloseReason, JustifiedCloseable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConnectionTasksThread private(consoleErr: RemoteConsole,
                                    ticketQueue: BlockingQueue[TaskTicket],
                                    lostInjections: mutable.Map[Int, ListBuffer[PacketInjection]]) extends Thread with JustifiedCloseable {

    @volatile private var open = false
    @volatile private var currentTicket: TaskTicket = _

    def this(consoleErr: RemoteConsole, identifier: String) = {
        this(consoleErr, new ArrayBlockingQueue[TaskTicket](15000), mutable.Map.empty)
        setName(s"RP Task Execution ($identifier)")
    }


    override def run(): Unit = {
        open = true
        while (open) {
            try {
                executeNextTicket()
            } catch {
                //normal exception thrown when the thread was suddenly stopped
                case _: InterruptedException =>
                case NonFatal(e) =>
                    e.printStackTrace()
                    consoleErr.print(e)
            }
        }
    }

    override def close(reason: CloseReason): Unit = {
        if (currentTicket != null) {
            currentTicket.abort(reason)
            currentTicket = null
        }

        ticketQueue.clear()
        lostInjections.clear()
        open = false

        interrupt()
    }

    def copy(): ConnectionTasksThread =
        new ConnectionTasksThread(consoleErr, ticketQueue, lostInjections)

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        ticketQueue.add(ticket)
    }

    private def executeNextTicket(): Unit = {
        val ticket = ticketQueue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID = channel.identifier
        //Adding eventual lost packets to this task
        if (lostInjections.contains(taskID)) {
            val queue = lostInjections(taskID)
            queue.foreach(channel.inject)
            queue.clear()
            lostInjections.remove(taskID)
        }
        ticket.start()
    }

    override def isClosed: Boolean = !open
}
