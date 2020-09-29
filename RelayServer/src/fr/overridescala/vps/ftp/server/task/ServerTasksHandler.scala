package fr.overridescala.vps.ftp.server.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.{DataPacket, Packet, Protocol, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.RelayServer

import scala.collection.mutable


class ServerTasksHandler(private val server: RelayServer) extends TasksHandler {

    private val clientsThreads = mutable.Map.empty[String, (ClientTasksThread, SocketChannel)]
    private val completersHandler = new ServerTaskCompleterHandler(this, server)

    override val identifier: String = Constants.SERVER_ID

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownFreeWill: Boolean, targetID: String, senderID: String = identifier): Unit = {
        val pair = clientsThreads(if (targetID.equals(identifier)) senderID else targetID)
        val thread = pair._1
        val socket = pair._2
        val ticket = new TaskTicket(executor, taskIdentifier, socket, ownFreeWill)
        thread.addTicket(ticket)
        println("new task registered !")
    }


    override def handlePacket(packet: Packet, senderID: String, socket: SocketChannel): Boolean = {
        println(s"handling packet $packet")
        checkOwner(senderID, socket)
        val thread = clientsThreads(senderID)._1
        if (packet.equals(Protocol.ABORT_TASK_PACKET)) {
            //Restarting the thread causes the current task to be skipped
            //And wait / execute the other task
            thread.close()
            thread.start()
            return false
        }
        try {
            packet match {
                case init: TaskInitPacket => completersHandler.handleCompleter(init, senderID)
                case data: DataPacket => thread.injectPacket(data)
            }
        } catch {
            case e: Throwable => e.printStackTrace()
                return true
        }
        false
    }

    def cancelTasks(ownerID: String): Unit = {
        if (!clientsThreads.contains(ownerID))
            return
        clientsThreads(ownerID)._1.close()
        clientsThreads.remove(ownerID)
    }

    private def checkOwner(ownerID: String, socket: SocketChannel): Unit = {
        if (!clientsThreads.contains(ownerID)) {
            val thread = new ClientTasksThread(ownerID)
            clientsThreads.put(ownerID, (thread, socket))
            thread.start()
        }
    }

    /**
     * @return the [[TaskCompleterHandler]]
     * @see [[TaskCompleterHandler]]
     * */
    override def getTasksCompleterHandler: TaskCompleterHandler = completersHandler
}