package fr.overridescala.vps.ftp.server

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.{ClientTaskThread, TaskCompleterFactory, TaskExecutor, TaskTicket, TasksHandler}

import scala.collection.mutable


class ServerTasksHandler() extends TasksHandler {

    private val clientThreads = mutable.Map.empty[String, (ClientTaskThread, SocketChannel)]

    override def registerTask(executor: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val pair = clientThreads(ownerID)
        val thread = pair._1
        val socket = pair._2
        val ticket = new TaskTicket(executor, sessionID, socket, ownerID, ownFreeWill)
        thread.addTicket(ticket)
    }

    //TODO
    //get la tache en cours d'execution
    //lui ajouter les packets si les ids correspondent
    //créer une nouvelle tâche si non
    def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, ownerID: String, socket: SocketChannel): Unit = {
        checkOwner(ownerID, socket)
        val completer = factory.getCompleter(packet)
        registerTask(completer, packet.sessionID, ownerID, false)
    }

    def cancelTasks(ownerID: String): Unit = {
        clientThreads(ownerID)._1.close()
        clientThreads.remove(ownerID)
    }

    private def checkOwner(ownerID: String, socket: SocketChannel): Unit = {
        if (!clientThreads.contains(ownerID)) {
            val thread = new ClientTaskThread
            clientThreads.put(ownerID, (thread, socket))
            thread.start()
        }
    }

}