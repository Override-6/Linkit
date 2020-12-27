package fr.`override`.linkit.server.task

import fr.`override`.linkit.api.exception.TaskException
import fr.`override`.linkit.api.packet.PacketCoordinates
import fr.`override`.linkit.api.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.api.system.{CloseReason, SystemOrder, SystemPacketChannel}
import fr.`override`.linkit.api.task.{TaskCompleterHandler, TaskExecutor, TaskTicket, TasksHandler}
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.connection.ClientConnection

/**
 * handle tasks between a RelayPoint.
 * @param server the server instance
 * */
class ConnectionTasksHandler(server: RelayServer,
                             systemChannel: SystemPacketChannel,
                             connection: ClientConnection) extends TasksHandler {

    private var tasksThread = new ConnectionTasksThread(connection)
    tasksThread.start()

    override val tasksCompleterHandler: TaskCompleterHandler = server.taskCompleterHandler
    private val errConsole = connection.getConsoleErr
    override val identifier: String = connection.identifier

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
                systemChannel.sendOrder(SystemOrder.ABORT_TASK, CloseReason.INTERNAL_ERROR)
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
    override def close(reason: CloseReason): Unit = {
        tasksThread.close(reason)
    }

    /**
     * Suddenly stop a task execution and execute his successor.
     * */
    override def skipCurrent(reason: CloseReason): Unit = {
        //Restarting the thread causes the current task to be skipped
        //And wait or execute the task that come after it
        val lastThread = tasksThread
        tasksThread = tasksThread.copy()
        lastThread.close(reason)
        tasksThread.start()
    }

}