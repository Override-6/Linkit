package fr.overridescala.vps.ftp.client

import java.io.Closeable
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, Packet, PacketChannelManager, Protocol, SimplePacketChannel, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

class ClientTasksHandler(private val socket: SocketChannel,
                         private val relay: RelayPoint) extends Thread with TasksHandler {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    override val tasksCompleterHandler = new ClientTaskCompleterHandler(relay)
    override val identifier: String = relay.identifier

    private var currentChannelManager: PacketChannelManager = _
    @volatile private var open = false;

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownFreeWill: Boolean): Unit = {
        val ticket = new TaskTicket(executor, taskIdentifier, ownFreeWill)
        queue.offer(ticket)
    }

    override def handlePacket(packet: Packet): Unit = {
        if (packet.equals(Protocol.ABORT_TASK_PACKET)) {
            //Restarting the thread causes the current task to be skipped
            //And wait / execute the other task
            close()
            start()
            return
        }
        try {
            packet match {
                case init: TaskInitPacket => tasksCompleterHandler.handleCompleter(init, identifier, this)
                case data: DataPacket if currentChannelManager != null => currentChannelManager.addPacket(data)
            }
        } catch {
            case e: TaskException =>
                socket.write(Protocol.ABORT_TASK_PACKET.toBytes)
                throw e
        }
    }

    override def run(): Unit = {
        open = true
        while (open) {
            val ticket = queue.take()
            if (!open) return
            currentChannelManager = ticket.channel
            ticket.start()
        }
        setName("Client Tasks")
    }


    override def close(): Unit = {
        open = false
        interrupt()
    }

    private class TaskTicket(private val executor: TaskExecutor,
                             private val taskID: Int,
                             private val ownFreeWill: Boolean) {

        val taskName: String = executor.getClass.getSimpleName
        private[ClientTasksHandler] val channel: SimplePacketChannel = new SimplePacketChannel(socket, taskID)

        def start(): Unit = {
            try {
                println(s"executing $taskName...")
                if (ownFreeWill) {
                    val initInfo = executor.initInfo
                    channel.sendInitPacket(initInfo)
                }
                executor.execute(channel)
                //println(s"$taskName completed !")
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }

        override def toString: String = s"Ticket(taskName = $taskName," +
                s" id = $taskID," +
                s" freeWill = $ownFreeWill)"

    }

}
