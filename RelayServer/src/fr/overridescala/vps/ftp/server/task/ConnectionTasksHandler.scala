package fr.overridescala.vps.ftp.server.task

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.PacketCoordinates
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.system.{Reason, RemoteConsole, SystemOrder, SystemPacketChannel}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TaskTicket, TasksHandler}
import fr.overridescala.vps.ftp.server.RelayServer

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
                errConsole.reportException(e)
        }
    }

    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit = {
        val linkedRelayID = if (ownFreeWill) targetID else senderID
        if (linkedRelayID == server.identifier)
            throw new TaskException("can't start a task from server to server !")

        val ticket = new TaskTicket(executor, server, taskIdentifier, linkedRelayID, ownFreeWill)
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