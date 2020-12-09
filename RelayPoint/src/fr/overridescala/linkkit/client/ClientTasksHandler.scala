package fr.overridescala.linkkit.client

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.linkkit.api.exceptions.TaskException
import fr.overridescala.linkkit.api.packet.PacketCoordinates
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket
import fr.overridescala.linkkit.api.system.{Reason, SystemOrder, SystemPacketChannel}
import fr.overridescala.linkkit.api.task.{TaskCompleterHandler, TaskExecutor, TaskTicket, TasksHandler}

import scala.util.control.NonFatal

protected class ClientTasksHandler(private val systemChannel: SystemPacketChannel,
                                   private val relay: RelayPoint) extends TasksHandler {

    private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](relay.configuration.taskQueueSize)
    private var tasksThread: Thread = _

    @volatile private var currentTicket: TaskTicket = _
    @volatile private var open = false

    override val tasksCompleterHandler = new TaskCompleterHandler()
    override val identifier: String = relay.identifier


    override def schedule(executor: TaskExecutor, taskIdentifier: Int, targetID: String, ownFreeWill: Boolean): Unit = {
        if (targetID == identifier)
            throw new TaskException("Can't start a task with oneself !")

        val ticket = new TaskTicket(executor, relay, taskIdentifier, targetID, ownFreeWill)
        queue.offer(ticket)
    }

    override def handlePacket(packet: TaskInitPacket, coordinates: PacketCoordinates): Unit = {
        try {
            tasksCompleterHandler.handleCompleter(packet, coordinates, this)
        } catch {
            case e: TaskException =>
                Console.err.println(e.getMessage)
                systemChannel.sendOrder(SystemOrder.ABORT_TASK, Reason.INTERNAL_ERROR)

                val errConsoleOpt = relay.getConsoleErr(coordinates.senderID)
                if (errConsoleOpt.isDefined)
                    errConsoleOpt.get.reportException(e)
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
