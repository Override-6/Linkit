package fr.overridescala.vps.ftp.client

import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskExecutor, TasksHandler}

 protected class ClientTasksHandler(private val socket: Socket,
                         private val relay: RelayPoint) extends Thread with TasksHandler {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val out = new BufferedOutputStream(socket.getOutputStream)


    private var currentChannelManager: PacketChannelManager = _
    @volatile private var open = false

    override val tasksCompleterHandler = new ClientTaskCompleterHandler(relay)
    override val identifier: String = relay.identifier

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit = {
        val ticket = new TaskTicket(executor, taskIdentifier, targetID, ownFreeWill)
        queue.offer(ticket)
    }

    override def handlePacket(packet: Packet): Unit = {
        if (isAbortPacket(packet)) {
            skipCurrent()
            return
        }
        try {
            packet match {
                case init: TaskInitPacket => tasksCompleterHandler.handleCompleter(init, this)
                case data: DataPacket if currentChannelManager != null => currentChannelManager.addPacket(data)
            }
        } catch {
            case e: TaskException =>
                out.write(DataPacket(Protocol.ERROR_ID, Protocol.ABORT_TASK, relay.identifier, identifier))
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
    }


    override def close(): Unit = {
        open = false
        interrupt()
    }

    private def isAbortPacket(packet: Packet): Boolean = {
        if (!packet.isInstanceOf[DataPacket])
            return false
        val dataPacket = packet.asInstanceOf[DataPacket]
        dataPacket.taskID == Protocol.ERROR_ID && dataPacket.header.equals(Protocol.ABORT_TASK)
    }

     override def skipCurrent(): Unit = {
         //Restarting the thread causes the current task to be skipped
         //And wait or execute the task that come after it
         println("skipping task...")
         close()
         start()
         println("task skipped !")
     }

    private class TaskTicket(private val executor: TaskExecutor,
                             private val taskID: Int,
                             private val targetID: String,
                             private val ownFreeWill: Boolean) {

        val taskName: String = executor.getClass.getSimpleName
        private[ClientTasksHandler] val channel: SimplePacketChannel = new SimplePacketChannel(socket, targetID, relay.identifier, taskID)

        def start(): Unit = {
            try {
                //println(s"executing $taskName...")
                if (ownFreeWill) {
                    val initInfo = executor.initInfo
                    channel.sendInitPacket(initInfo)
                }
                executor.execute(channel)
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }

        override def toString: String = s"Ticket(taskName = $taskName," +
                s" id = $taskID," +
                s" freeWill = $ownFreeWill)"

    }
     setName("Client Tasks scheduler")
 }
