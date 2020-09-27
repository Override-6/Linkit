package fr.overridescala.vps.ftp.client

import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, Packet, PacketChannelManager, SimplePacketChannel, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

class ClientTasksHandler(private val socket: SocketChannel,
                         private val relay: RelayPoint) extends TasksHandler {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private var currentChannelManager: PacketChannelManager = _
    private val completerFactory = new ClientTaskCompleterHandler(this, relay)

    override val identifier: String = relay.identifier

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownFreeWill: Boolean, targetID: String, senderID: String = identifier): Unit = {
        val ticket = new TaskTicket(executor, senderID, taskIdentifier, ownFreeWill)
        queue.offer(ticket)
        println("new task registered !")
    }

    override def handlePacket(packet: Packet, ownerID: String, socket: SocketChannel): Unit = {
        if (packet.isInstanceOf[TaskInitPacket]) {
            completerFactory.handleCompleter(packet.asInstanceOf[TaskInitPacket], ownerID)
            return
        }
        currentChannelManager.addPacket(packet)
    }

    override def getTasksCompleterHandler: TaskCompleterHandler = completerFactory

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

    private class TaskTicket(private val taskAchiever: TaskExecutor,
                             private val ownerID: String,
                             private val taskID: Int,
                             private val ownFreeWill: Boolean) {

        val taskName: String = taskAchiever.getClass.getSimpleName
        private[ClientTasksHandler] val channel: SimplePacketChannel = new SimplePacketChannel(socket, taskID)

        def start(): Unit = {
            try {
                println(s"executing $taskName...")
                if (ownFreeWill)
                    taskAchiever.sendTaskInfo(channel)
                taskAchiever.execute(channel)
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
