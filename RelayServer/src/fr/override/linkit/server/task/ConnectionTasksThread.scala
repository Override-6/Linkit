package fr.`override`.linkit.server.task

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable, RemoteConsole}
import fr.`override`.linkit.api.task.TaskTicket

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConnectionTasksThread private(consoleErr: RemoteConsole,
                                    ticketQueue: BlockingQueue[TaskTicket],
                                    lostPackets: mutable.Map[Int, ListBuffer[(Packet, PacketCoordinates)]]) extends Thread with JustifiedCloseable {

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
        lostPackets.clear()
        open = false

        interrupt()
    }

    def copy(): ConnectionTasksThread =
        new ConnectionTasksThread(consoleErr, ticketQueue, lostPackets)

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        ticketQueue.add(ticket)
    }

    private def executeNextTicket(): Unit = {
        val ticket = ticketQueue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID = channel.identifier
        //Adding eventual lost packets to this task
        if (lostPackets.contains(taskID)) {
            val queue = lostPackets(taskID)
            queue.foreach(element => channel.injectPacket(element._1, element._2))
            queue.clear()
            lostPackets.remove(taskID)
        }
        ticket.start()
    }

    override def isClosed: Boolean = !open
}
