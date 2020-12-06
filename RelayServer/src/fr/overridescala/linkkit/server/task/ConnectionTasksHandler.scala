package fr.overridescala.linkkit.server.task

import fr.overridescala.linkkit.api.exceptions.TaskException
import fr.overridescala.linkkit.api.packet.PacketCoordinates
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket
import fr.overridescala.linkkit.api.system.{Reason, RemoteConsole, SystemOrder, SystemPacketChannel}
import fr.overridescala.linkkit.api.task.{TaskCompleterHandler, TaskExecutor, TaskTicket, TasksHandler}
import fr.overridescala.linkkit.server.RelayServer

/**
 * handle tasks between a RelayPoint.
 * @param identifier the connected RelayPoint identifier.
 * @param server the server instance
 * */
class ConnectionTasksHandler(override val identifier: String,
                             server: RelayServer,
                             systemChannel: SystemPacketChannel,
                             errConsole: RemoteConsole.Err) extends TasksHandler {

    private var tasksThread = new ConnectionTasksThread(identifier, errConsole)
    tasksThread.start()

    override val tasksCompleterHandler: TaskCompleterHandler = server.taskCompleterHandler

    /**
     * Handles the packet.
     * @param packet packet to handle
     *
     * @throws TaskException if the handling went wrong
     * */
    override def handlePacket(packet: TaskInitPacket, coordinates: PacketCoordinates): Unit = {
        try {
            tasksCompleterHandler.handleCompleter(packet, coordinates, this)
        } catch {
            case e: TaskException =>
                Console.err.println(e.getMessage)
                systemChannel.sendOrder(SystemOrder.ABORT_TASK, Reason.INTERNAL_ERROR)
                errConsole.reportExceptionSimplified(e)
        }
    }

    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    override def schedule(executor: TaskExecutor, taskIdentifier: Int, targetID: String, ownFreeWill: Boolean): Unit = {
        if (targetID == server.identifier)
            throw new TaskException("can't schedule any task execution from server to server !")

        val ticket = new TaskTicket(executor, server, taskIdentifier, targetID, ownFreeWill)
        tasksThread.addTicket(ticket)
    }

    /**
     * closes the current client tasks thread
     * */
    override def close(reason: Reason): Unit = {
        tasksThread.close(reason)
    }

    /**
     * Suddenly stop a task execution and execute his successor.
     * */
    override def skipCurrent(reason: Reason): Unit = {
        //Restarting the thread causes the current task to be skipped
        //And wait or execute the task that come after it
        val lastThread = tasksThread
        tasksThread = tasksThread.copy()
        lastThread.close(reason)
        tasksThread.start()
    }

}