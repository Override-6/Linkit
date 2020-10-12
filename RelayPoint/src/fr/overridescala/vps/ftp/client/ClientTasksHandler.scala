package fr.overridescala.vps.ftp.client

import java.io.BufferedOutputStream
import java.lang.reflect.InvocationTargetException
import java.net.Socket
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.client.tasks.InitTaskCompleter

import scala.util.control.NonFatal

protected class ClientTasksHandler(private val socket: Socket,
                                   private val relay: RelayPoint) extends TasksHandler {

    private val packetManager = relay.getPacketManager
    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private val out = new BufferedOutputStream(socket.getOutputStream)
    private var tasksThread: Thread = _

    @volatile private var currentTicket: TaskTicket = _
    @volatile private var open = false

    override val tasksCompleterHandler = new TaskCompleterHandler()
    tasksCompleterHandler.putCompleter(InitTaskCompleter.TYPE, _ => new InitTaskCompleter(relay))

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
                case other: Packet if currentTicket != null => currentTicket.channel.addPacket(other)
                case _: Packet => Console.err.println("could not handle packet " + packet)
            }
        } catch {
            case e: TaskException =>
                val errorPacket = new ErrorPacket(-1,
                    relay.identifier,
                    packet.senderIdentifier,
                    ErrorPacket.ABORT_TASK,
                    e.getMessage)
                out.write(packetManager.toBytes(errorPacket))
                out.flush()
        }
    }


    override def close(): Unit = {
        if (currentTicket != null) {
            currentTicket.abort()
            currentTicket = null
        }
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
            new SimplePacketChannel(socket, targetID, relay.identifier, taskID, packetManager)

        def abort(): Unit = {
            notifyExecutor()
            executor match {
                case task: Task[_] =>
                    val errorMethod = task.getClass.getMethod("error", classOf[String])
                    errorMethod.setAccessible(true)
                    try {
                        errorMethod.invoke(task, "Task aborted from an external handler")
                    } catch {
                        case e: InvocationTargetException if e.getCause.getClass == classOf[TaskException] =>
                    }
                case _ =>
            }
        }

        def start(): Unit = {
            try {
                if (ownFreeWill) {
                    val initInfo = executor.initInfo
                    channel.sendInitPacket(initInfo)
                }
                executor.init(packetManager, channel)
                executor.execute()
                notifyExecutor()
            } catch {
                case _: InterruptedException => Console.err.println(s"$taskName execution suddenly ended")
                case NonFatal(e) => e.printStackTrace()
            }
        }

        private def notifyExecutor(): Unit = executor.synchronized {
            executor.notifyAll()
        }

    }

}
