package fr.overridescala.vps.ftp.server.task

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannelManager}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ClientTasksThread(ownerID: String) extends Thread with Closeable {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val lostPackets: mutable.Map[Int, ListBuffer[DataPacket]] = mutable.Map.empty
    @volatile private var open = false
    @volatile private var currentChannelManager: PacketChannelManager = _

    override def run(): Unit = {
        open = true
        while (open) {
            try {
                val ticket = queue.take()
                currentChannelManager = ticket.channel
                val taskID = currentChannelManager.taskID
                if (lostPackets.contains(taskID)) {
                    val queue = lostPackets(taskID)
                    queue.foreach(currentChannelManager.addPacket)
                    queue.clear()
                    lostPackets.remove(taskID)
                }
                ticket.start()
            } catch {
                //normal exception thrown when the thread was suddenly stopped
                case e: InterruptedException => e.printStackTrace()
            }
        }
    }

    override def close(): Unit = {
        open = false
        interrupt()
    }

    def injectPacket(packet: DataPacket): Unit = {
        if (canInject(packet)) {
            currentChannelManager.addPacket(packet)
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

    def addTicket(ticket: TaskTicket): Unit = {
        queue.add(ticket)
    }

    private def canInject(packet: DataPacket): Boolean =
        currentChannelManager != null && currentChannelManager.taskID == packet.taskID

    setName(s"RP Task Execution ($ownerID)")

}
