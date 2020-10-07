package fr.overridescala.vps.ftp.server.task

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannelManager}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ClientTasksThread(ownerID: String) extends Thread with Closeable with Cloneable {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val lostPackets: mutable.Map[Int, ListBuffer[DataPacket]] = mutable.Map.empty

    @volatile private var open = false
    @volatile private var currentTicket: TaskTicket = _

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
        if (currentTicket != null)
            currentTicket.notifyExecutor()
        queue.clear()
        lostPackets.clear()
        open = false
        interrupt()
    }

    private[task] def injectPacket(packet: DataPacket): Unit = {
        if (canInject(packet)) {
            val channel = currentTicket.channel
            channel.addPacket(packet)
            return
        }
        val packetTaskID = packet.taskID
        if (lostPackets.contains(packetTaskID)) {
            lostPackets(packetTaskID).addOne(packet)
            return
        }

        val lost = ListBuffer.empty[DataPacket]
        lost += packet
        lostPackets.put(packetTaskID, lost)
    }

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        queue.add(ticket)
    }

    override def clone(): ClientTasksThread =
        super.clone().asInstanceOf[ClientTasksThread]

    private def executeNextTicket(): Unit = {
        val ticket = queue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID = channel.taskID
        if (lostPackets.contains(taskID)) {
            val queue = lostPackets(taskID)
            queue.foreach(channel.addPacket)
            queue.clear()
            lostPackets.remove(taskID)
        }
        ticket.start()
    }

    private def canInject(packet: DataPacket): Boolean = {
        val channel = currentTicket.channel
        currentTicket != null && channel.taskID == packet.taskID
    }

    setName(s"RP Task Execution ($ownerID)")

}
