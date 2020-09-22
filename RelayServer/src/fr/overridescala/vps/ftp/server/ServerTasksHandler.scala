package fr.overridescala.vps.ftp.server

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.{TaskCompleterFactory, TaskExecutor, TasksHandler}

import scala.collection.mutable


class ServerTasksHandler() extends TasksHandler {

    private val clientsThreads = mutable.Map.empty[String, (ClientTasksThread, SocketChannel)]

    override def registerTask(executor: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val pair = clientsThreads(ownerID)
        val thread = pair._1
        val socket = pair._2
        val ticket = new TaskTicket(executor, sessionID, socket, ownerID, ownFreeWill)
        thread.addTicket(ticket)
        println("new task registered !")
    }


    override def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, ownerID: String, socket: SocketChannel): Unit = {
        checkOwner(ownerID, socket)
        val thread = clientsThreads(ownerID)._1

        if (thread.tasksIDMatches(packet)) {
            thread.injectPacket(packet)
            return
        }

        val completer = factory.getCompleter(packet)
        registerTask(completer, packet.taskID, ownerID, false)

    }

    def cancelTasks(ownerID: String): Unit = {
        if (!clientsThreads.contains(ownerID))
            return
        clientsThreads(ownerID)._1.close()
        clientsThreads.remove(ownerID)
    }

    private def checkOwner(ownerID: String, socket: SocketChannel): Unit = {
        if (!clientsThreads.contains(ownerID)) {
            val thread = new ClientTasksThread
            clientsThreads.put(ownerID, (thread, socket))
            thread.start()
        }
    }

}