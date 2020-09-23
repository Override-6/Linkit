package fr.overridescala.vps.ftp.server

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}

import scala.collection.mutable


class ServerTasksHandler(private val server: RelayServer) extends TasksHandler {

    private val clientsThreads = mutable.Map.empty[String, (ClientTasksThread, SocketChannel)]
    private val completerFactory = new ServerTaskCompleterHandler(this, server)

    override def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val pair = clientsThreads(ownerID)
        val thread = pair._1
        val socket = pair._2
        val ticket = new TaskTicket(executor, taskIdentifier, socket, ownFreeWill)
        thread.addTicket(ticket)
        println("new task registered !")
    }


    override def handlePacket(packet: DataPacket, ownerID: String, socket: SocketChannel): Unit = {
        checkOwner(ownerID, socket)
        val thread = clientsThreads(ownerID)._1

        if (thread.tasksIDMatches(packet)) {
            thread.injectPacket(packet)
            return
        }
        completerFactory.handleCompleter(packet, ownerID)
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
    override def getTasksCompleterHandler: TaskCompleterHandler = ???
}