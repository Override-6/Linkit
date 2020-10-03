package fr.overridescala.vps.ftp.server.task

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskExecutor, TasksHandler}

/**
 * handle tasks between a RelayPoint.
 * @param identifier the connected RelayPoint identifier.
 * @param server the server instance
 * @param socketWriter the writer in which tasks will write packets
 * */
class ClientTasksHandler(override val identifier: String,
                         private val server: Relay,
                         private val socketWriter: SocketWriter) extends TasksHandler {


    private val tasksThread = new ClientTasksThread(identifier)
    override val tasksCompleterHandler = new ServerTaskCompleterHandler(this, server)

    /**
     * Handles the packet.
     * @param packet packet to handle
     *
     * @throws TaskException if the handling went wrong
     * */
    override def handlePacket(packet: Packet): Unit = {
        if (packet.equals(Protocol.ABORT_TASK_PACKET)) {
            //Restarting the thread causes the current task to be skipped
            //And wait / execute the other task
            tasksThread.close()
            tasksThread.start()
            return
        }
        try {
            packet match {
                case init: TaskInitPacket => tasksCompleterHandler.handleCompleter(init, identifier)
                case data: DataPacket => tasksThread.injectPacket(data)
            }
        } catch {
            case e: TaskException =>
                socketWriter.write(Protocol.ABORT_TASK_PACKET.toBytes)
                throw e
        }
    }

    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    override def registerTask(executor: TaskExecutor,
                              taskIdentifier: Int,
                              ownFreeWill: Boolean): Unit =
        tasksThread.addTicket(new TaskTicket(executor, taskIdentifier, socketWriter, ownFreeWill))

    def clearTasks(): Unit =
        tasksThread.close()

}