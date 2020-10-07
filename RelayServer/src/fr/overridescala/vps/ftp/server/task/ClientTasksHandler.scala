package fr.overridescala.vps.ftp.server.task

import java.io.BufferedOutputStream
import java.net.Socket

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

/**
 * handle tasks between a RelayPoint.
 * @param identifier the connected RelayPoint identifier.
 * @param server the server instance
 * @param socket the writer in which tasks will write packets
 * */
class ClientTasksHandler(override val identifier: String,
                         private val server: Relay,
                         private val socket: Socket) extends TasksHandler {


    private val out = new BufferedOutputStream(socket.getOutputStream)
    private var tasksThread = new ClientTasksThread(identifier)
    tasksThread.start()


    override val tasksCompleterHandler: TaskCompleterHandler = server.getTaskCompleterHandler

    /**
     * Handles the packet.
     * @param packet packet to handle
     *
     * @throws TaskException if the handling went wrong
     * */
    override def handlePacket(packet: Packet): Unit = {
        try {
            packet match {
                case init: TaskInitPacket => tasksCompleterHandler.handleCompleter(init, this)
                case data: DataPacket => tasksThread.injectPacket(data)
            }
        } catch {
            case e: TaskException =>
                out.write(Protocol.ABORT_TASK.getBytes)
                e.printStackTrace()
        }
    }

    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, targetID: String, senderID: String, ownFreeWill: Boolean): Unit =
        tasksThread.addTicket(new TaskTicket(executor, taskIdentifier, identifier, senderID, socket, ownFreeWill))

    /**
     * closes the current client tasks thread
     * */
    override def close(): Unit = {
        tasksThread.close()
        out.close()
    }

    /**
     * Suddenly stop a task execution and execute his successor.
     * */
    override def skipCurrent(): Unit = {
        //Restarting the thread causes the current task to be skipped
        //And wait or execute the task that come after it
        val lastThread = tasksThread
        tasksThread = tasksThread.clone()
        lastThread.close()
        tasksThread.start()
    }

}