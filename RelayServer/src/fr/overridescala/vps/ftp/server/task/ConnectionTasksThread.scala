package fr.overridescala.vps.ftp.server.task

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.Packet

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConnectionTasksThread private(ownerID: String,
                                    ticketQueue: BlockingQueue[TaskTicket],
                                    lostPackets: mutable.Map[Int, ListBuffer[Packet]]) extends Thread with Closeable {

    @volatile private var open = false
    @volatile private var currentTicket: TaskTicket = _

    def this(ownerID: String) =
        this(ownerID, new ArrayBlockingQueue[TaskTicket](200), mutable.Map.empty)


    override def run(): Unit = {
        open = true
        while (open) {
            try {
                executeNextTicket()
            } catch {
                //normal exception thrown when the thread was suddenly stopped
                case e: InterruptedException =>
                case NonFatal(e) => e.printStackTrace()
            }
        }
    }

    override def close(): Unit = {
        if (currentTicket != null) {
            currentTicket.abort()
            currentTicket = null
        }
        ticketQueue.clear()
        lostPackets.clear()
        open = false
        interrupt()
    }

    def copy(): ConnectionTasksThread =
        new ConnectionTasksThread(ownerID, ticketQueue, lostPackets)

    private[task] def injectPacket(packet: Packet): Unit = {
        if (canInject(packet)) {
            val channel = currentTicket.channel
            channel.addPacket(packet)
            return
        }
        val packetTaskID = packet.channelID
        if (lostPackets.contains(packetTaskID)) {
            lostPackets(packetTaskID).addOne(packet)
            return
        }

        val lost = ListBuffer.empty[Packet]
        lost += packet
        lostPackets.put(packetTaskID, lost)
    }

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        ticketQueue.add(ticket)
    }

    private def executeNextTicket(): Unit = {
        val ticket = ticketQueue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID = channel.channelID
        //Adding eventual lost packets to this task
        if (lostPackets.contains(taskID)) {
            val queue = lostPackets(taskID)
            queue.foreach(channel.addPacket)
            queue.clear()
            lostPackets.remove(taskID)
        }
        ticket.start()
    }

    private def canInject(packet: Packet): Boolean =
        currentTicket != null && currentTicket.channel.channelID == packet.channelID

    setName(s"RP Task Execution ($ownerID)")

}
