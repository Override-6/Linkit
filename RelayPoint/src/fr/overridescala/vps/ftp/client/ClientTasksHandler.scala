package fr.overridescala.vps.ftp.client

import java.io.Closeable
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, Packet, PacketChannelManager, Protocol, SimplePacketChannel, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

class ClientTasksHandler(private val socket: SocketChannel,
                         private val relay: RelayPoint) extends Thread with TasksHandler with Closeable {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val completerHandler = new ClientTaskCompleterHandler(this, relay)
    override val identifier: String = relay.identifier

    private var currentChannelManager: PacketChannelManager = _
    @volatile private var open = false;

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownFreeWill: Boolean, targetID: String, senderID: String = identifier): Unit = {
        val ticket = new TaskTicket(executor, senderID, taskIdentifier, ownFreeWill)
        queue.offer(ticket)
        println("new task registered !")
    }

    override def handlePacket(packet: Packet, senderId: String, socket: SocketChannel): Unit = {
        println(s"packet = ${packet}")
        println(s"Protocol.ABORT_TASK_PACKET = ${Protocol.ABORT_TASK_PACKET}")
        if (packet.equals(Protocol.ABORT_TASK_PACKET)) {
            //Restarting the thread causes the current task to be skipped
            //And wait / execute the other task
            close()
            start()
            return
        }
        try {
            packet match {
                case init: TaskInitPacket => completerHandler.handleCompleter(init, senderId)
                case data: DataPacket if currentChannelManager != null => currentChannelManager.addPacket(data)
            }
        } catch {
            case e: TaskException =>
                socket.write(Protocol.ABORT_TASK_PACKET.toBytes)
                throw e
        }
    }

    override def getTasksCompleterHandler: TaskCompleterHandler = completerHandler

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
                             private val ownerID: String,
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
                println(s"$taskName completed !")
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }

        override def toString: String = s"Ticket(taskName = $taskName," +
                s" ownerID = $ownerID," +
                s" id = $taskID," +
                s" freeWill = $ownFreeWill)"

    }

}
