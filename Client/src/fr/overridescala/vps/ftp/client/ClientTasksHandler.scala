package fr.overridescala.vps.ftp.client

import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, Packet, PacketChannelManager, SimplePacketChannel, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

class ClientTasksHandler(private val socket: SocketChannel,
                         private val relay: RelayPoint) extends TasksHandler {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private var currentChannelManager: PacketChannelManager = _
    private val completerHandler = new ClientTaskCompleterHandler(this, relay)

    override val identifier: String = relay.identifier

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownFreeWill: Boolean, targetID: String, senderID: String = identifier): Unit = {
        val ticket = new TaskTicket(executor, senderID, taskIdentifier, ownFreeWill)
        queue.offer(ticket)
        println("new task registered !")
    }

    override def handlePacket(packet: Packet, senderId: String, socket: SocketChannel): Unit = {
        packet match {
            case init: TaskInitPacket => completerHandler.handleCompleter(init, senderId)
            case data: DataPacket if currentChannelManager != null => currentChannelManager.addPacket(data)
        }
    }

    override def getTasksCompleterHandler: TaskCompleterHandler = completerHandler

    def start(): Unit = {
        val thread = new Thread(() => {
            while (true) {
                val ticket = queue.take()
                currentChannelManager = ticket.channel
                ticket.start()
            }
        })
        thread.setName("Client Tasks")
        thread.start()
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
