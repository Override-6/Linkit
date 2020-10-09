package fr.overridescala.vps.ftp.client

import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskExecutor, TasksHandler}

import scala.util.control.NonFatal

protected class ClientTasksHandler(private val socket: Socket,
                                   private val relay: RelayPoint) extends TasksHandler {

    private val packetManager = relay.getPacketManager
    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val out = new BufferedOutputStream(socket.getOutputStream)
    private var tasksThread: Thread = _

    private var currentTicket: TaskTicket = _
    @volatile private var open = false

    override val tasksCompleterHandler = new ClientTaskCompleterHandler(relay)
    override val identifier: String = relay.identifier

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit = {
        val ticket = TaskTicket(executor, taskIdentifier, targetID, ownFreeWill)
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
                case data: DataPacket if currentTicket != null => currentTicket.channel.addPacket(data)
            }
        } catch {
            case e: TaskException =>
                val packet = ErrorPacket(ErrorPacket.ABORT_TASK, e.getMessage, identifier)
                out.write(packetManager.toBytes(packet))
                throw e
        }
    }


    override def close(): Unit = {
        if (currentTicket != null)
            currentTicket.notifyExecutor()
        open = false
        tasksThread.interrupt()
    }

    override def skipCurrent(): Unit = {
        //Restarting the thread causes the current task to be skipped
        //And wait or execute the task that come after it
        close()
        start()
    }

    private def listen(): Unit = {
        open = true
        while (open)
            executeNextTask()
    }


    private def executeNextTask(): Unit = {
        try {
            val ticket = queue.take()
            if (!open) return
            currentTicket = ticket
            ticket.start()
        } catch {
            //Do not considerate InterruptedException
            case _: InterruptedException =>
            case NonFatal(e) => e.printStackTrace()
        }
    }

    private def isAbortPacket(packet: Packet): Boolean = {
        if (!packet.isInstanceOf[ErrorPacket])
            return false
        packet.asInstanceOf[ErrorPacket].errorType == ErrorPacket.ABORT_TASK
    }

    def start(): Unit = {
        tasksThread = new Thread(() => listen())
        tasksThread.setName("Client Tasks scheduler")
        tasksThread.start()
    }

    private case class TaskTicket(private val executor: TaskExecutor,
                                  private val taskID: Int,
                                  private val targetID: String,
                                  private val ownFreeWill: Boolean) {

        val taskName: String = executor.getClass.getSimpleName
        private[ClientTasksHandler] val channel: SimplePacketChannel =
            new SimplePacketChannel(socket, targetID, relay.identifier, packetManager, taskID)

        def notifyExecutor(): Unit = executor.synchronized {
            executor.notifyAll()
        }

        def start(): Unit = {
            try {
                if (ownFreeWill) {
                    val initInfo = executor.initInfo
                    channel.sendInitPacket(initInfo)
                }
                executor.execute(channel)
                notifyExecutor()
            } catch {
                case _: InterruptedException => Console.err.println(s"$taskName execution suddenly ended")
                case NonFatal(e) => e.printStackTrace()
            }
        }

    }

}
