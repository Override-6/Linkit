package fr.overridescala.vps.ftp.client

import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannelManager, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

class ClientTasksHandler(socket: SocketChannel) extends TasksHandler{

    private val queue: BlockingQueue[TaskAchieverTicket] = new ArrayBlockingQueue[TaskAchieverTicket](200)
    private var currentChannelManager: PacketChannelManager = _
    private val completerFactory = new ClientTaskCompleterHandler(this)

    override def registerTask(achiever: TaskExecutor, taskIdentifier: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val ticket = new TaskAchieverTicket(achiever, ownerID, taskIdentifier, ownFreeWill)
        queue.offer(ticket)
        println("new task registered !")
    }

    override def handlePacket(packet: DataPacket, ownerID: String, socket: SocketChannel): Unit = {
        if (packet.taskID != currentChannelManager.taskID) {
            completerFactory.handleCompleter(packet)
            return
        }
        currentChannelManager.addPacket(packet)
    }

    override def getTaskCompleterFactory: TaskCompleterHandler = completerFactory

    def start(): Unit = {
        val thread = new Thread(() => {
            while (true) {
                println("waiting for another task to complete...")
                val ticket = queue.take()
                currentChannelManager = ticket.channel
                ticket.start()
            }
        })
        thread.setName("Client Tasks")
        thread.start()
    }

    private class TaskAchieverTicket(private val taskAchiever: TaskExecutor,
                                     private val ownerID: String,
                                     private val taskID: Int,
                                     private val ownFreeWill: Boolean) {

        val name: String = taskAchiever.getClass.getSimpleName
        private[ClientTasksHandler] val channel: SimplePacketChannel = new SimplePacketChannel(socket, ownerID, taskID)

        def start(): Unit = {
            try {
                println(s"executing $name...")
                if (ownFreeWill)
                    taskAchiever.sendTaskInfo(channel)
                taskAchiever.execute(channel)
            } catch {

                case e: Throwable => e.printStackTrace()
            }
        }

        override def toString: String = s"Ticket(name = $name," +
                s" ownerID = $ownerID," +
                s" id = $taskID," +
                s" freeWill = $ownFreeWill)"

    }

}
