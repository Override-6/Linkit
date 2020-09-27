package fr.overridescala.vps.ftp.server.task

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannelManager}

class ClientTasksThread(ownerID: String) extends Thread with Closeable {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    @volatile private var open = false
    @volatile private var currentChannelManager: PacketChannelManager = _

    override def run(): Unit = {
        open = true
        while (open) {
            try {
                val ticket = queue.take()
                currentChannelManager = ticket.channel
                ticket.start()
            } catch {
                //normal exception thrown when the thread was suddenly stopped
                case _:InterruptedException =>
            }
        }
    }

    override def close(): Unit = {
        open = false
        interrupt()
    }

    def injectPacket(packet: DataPacket): Unit =
        currentChannelManager.addPacket(packet)


    def addTicket(ticket: TaskTicket): Unit = {
        queue.add(ticket)
    }

    def tasksIDMatches(packet: DataPacket): Boolean = {
        currentChannelManager != null && packet.taskID == currentChannelManager.taskID
    }

    setName(s"RP Task Execution ($ownerID)")

}
