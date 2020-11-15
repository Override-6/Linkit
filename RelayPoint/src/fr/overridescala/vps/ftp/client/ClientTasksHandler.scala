package fr.overridescala.vps.ftp.client

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.exceptions.{TaskException, TaskOperationFailException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.fundamental.{ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.system.Reason
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TaskTicket, TasksHandler}

import scala.util.control.NonFatal

protected class ClientTasksHandler(private val socket: DynamicSocket,
                                   private val relay: RelayPoint) extends TasksHandler {

    private val packetManager = relay.packetManager
    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](200)
    private var tasksThread: Thread = _
    private val notifier: EventNotifier = relay.eventDispatcher.notifier

    @volatile private var currentTicket: TaskTicket = _
    @volatile private var open = false

    override val tasksCompleterHandler = new TaskCompleterHandler()
    override val identifier: String = relay.identifier


    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit = {
        val linkedRelay = if (ownFreeWill) targetID else senderID
        if (linkedRelay == identifier)
            throw new TaskOperationFailException("can't start a task with oneself !")

        val channel = relay.createSyncChannel0(linkedRelay, taskIdentifier)
        val ticket = new TaskTicket(executor, channel, packetManager, ownFreeWill)
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
                notifier.onSystemError(e)
        }
    }


    override def close(reason: Reason): Unit = {
        if (currentTicket != null) {
            currentTicket.abort(reason)
            currentTicket = null
        }
        open = false
        tasksThread.interrupt()
    }

    override def skipCurrent(reason: Reason): Unit = {
        //Restarting the thread causes the current task to be skipped
        //And wait or execute the task that come after it
        close(reason)
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

}
