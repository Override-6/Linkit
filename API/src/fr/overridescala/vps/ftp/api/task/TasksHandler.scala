package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.{DataPacket, SimplePacketChannel}

import scala.collection.mutable


class TasksHandler() {

    private val clientThreads = mutable.Map.empty[String, (ClientTaskThread, SocketChannel)]

    def register(executor: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val pair = clientThreads(ownerID)
        val thread = pair._1
        val ticket = new TaskTicket(executor, sessionID, pair._2, ownerID, ownFreeWill)
        thread.addTicket(ticket)
    }

    def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, ownerID: String, socket: SocketChannel): Unit = {

        if (!clientThreads.contains(ownerID)) {
            val thread = new ClientTaskThread
            clientThreads.put(ownerID, (thread, socket))
            thread.start()
        }
        val completer = factory.getCompleter(packet)
        register(completer, packet.sessionID, ownerID, false)

    }

    def cancelTasks(ownerID: String): Unit = {
        clientThreads(ownerID)._1.close()
        clientThreads.remove(ownerID)
    }

}