package fr.overridescala.vps.ftp.client

import java.io.BufferedOutputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.{TaskException, TaskOperationException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler, TaskExecutor, TasksHandler}

import scala.util.control.NonFatal

protected class ClientTasksHandler(private val socket: DynamicSocket,
                                   private val relay: RelayPoint) extends TasksHandler {

    private val packetManager = relay.packetManager
    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private var tasksThread: Thread = _

    @volatile private var currentTicket: TaskTicket = _
    @volatile private var open = false

    override val tasksCompleterHandler = new TaskCompleterHandler()
    override val identifier: String = relay.identifier

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit = {
        val linkedRelay = if (ownFreeWill) targetID else senderID
        if (linkedRelay == identifier)
            throw new TaskOperationException("can't start a task with oneself !")

        val ticket = TaskTicket(executor, taskIdentifier, linkedRelay, ownFreeWill)
        queue.offer(ticket)
    }

    override def handlePacket(packet: TaskInitPacket): Unit = {
        try {
            tasksCompleterHandler.handleCompleter(packet, this)
        } catch {
            case e: TaskException =>
                val msg = e.getMessage
                Console.err.println(msg)
                val errorPacket = new ErrorPacket(-1,
                    relay.identifier,
                    packet.senderID,
                    ErrorPacket.ABORT_TASK,
                    msg)
                socket.write(packetManager.toBytes(errorPacket))
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

    def start(): Unit = {
        tasksThread = new Thread(() => listen())
        tasksThread.setName("Client Tasks scheduler")
        tasksThread.start()
    }

    private case class TaskTicket(private val executor: TaskExecutor,
                                  private val taskID: Int,
                                  private val linkedRelay: String,
                                  private val ownFreeWill: Boolean) {

        val taskName: String = executor.getClass.getSimpleName

        private[ClientTasksHandler] val channel: SyncPacketChannel =
            relay.createSyncChannel0(linkedRelay, taskID)

        def abort(): Unit = {
            notifyExecutor()
            executor match {
                case task: Task[_] =>
                    val errorMethod = task.getClass.getMethod("error", classOf[String])
                    errorMethod.setAccessible(true)
                    try {
                        errorMethod.invoke(task, "Task aborted from an external handler")
                    } catch {
                        case e: InvocationTargetException if e.getCause.isInstanceOf[TaskException] => Console.err.println(e.getMessage)
                        case e: InvocationTargetException => e.getCause.printStackTrace()
                        case NonFatal(e) => e.printStackTrace()
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
            } catch {
                case e: TaskOperationException => Console.err.println(e.getMessage)
                case _: InterruptedException => Console.err.println(s"$taskName execution suddenly ended")
                case NonFatal(e) => e.printStackTrace()
            } finally {
                notifyExecutor()
                executor.closeChannel()
            }
        }

        private def notifyExecutor(): Unit = executor.synchronized {
            executor.notifyAll()
        }

    }

}
