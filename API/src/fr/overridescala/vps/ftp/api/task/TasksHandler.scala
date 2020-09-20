package fr.overridescala.vps.ftp.api.task

import java.net.SocketAddress
import java.util

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel, PacketChannelManager, SimplePacketChannel}


class TasksHandler() {

    private val queue: util.Queue[TaskAchieverTicket] = new util.ArrayDeque[TaskAchieverTicket]()
    var currentSessionID: Int = -1
    private var currentTaskOwner: String = _


    def register(achiever: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val ticket = new TaskAchieverTicket(achiever, ownerID, sessionID, ownFreeWill)
        queue.offer(ticket)
        println()
        synchronized(notifyAll())
    }

    def start(): Unit = {
        val thread = new Thread(() => {
            while (true) {
                startNextTask()
                println("waiting for another task to complete...")
            }
        })
        thread.setName("Tasks")
        thread.start()
    }

    def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, channel: SimplePacketChannel): Unit = {
        if (packet.sessionID != currentSessionID) {
            val completer = factory.getCompleter(channel, packet)
            register(completer, packet.sessionID, channel.identifier, false)
            return
        }
        channel.addPacket(packet)
    }

    def cancelTasks(ownerID: String): Unit = {
        if (currentTaskOwner != null && currentTaskOwner.equals(ownerID)) {
            synchronized(notifyAll())
            currentTaskOwner = ""
        }
        queue.removeIf(_.isOwner(ownerID))
    }

    private def startNextTask(): Unit = {
        val ticket = queue.poll()
        if (ticket == null) {
            synchronized {
                wait()
            }
            startNextTask()
            return
        }
        ticket.start()
    }

    private class TaskAchieverTicket(val taskAchiever: TaskExecutor,
                                     val ownerID: String,
                                     val sessionID: Int,
                                     val ownFreeWill: Boolean) {

        val name: String = taskAchiever.getClass.getSimpleName

        def isOwner(id: String): Boolean = id.equals(ownerID)

        def start(): Unit = {
            currentTaskOwner = ownerID
            currentSessionID = sessionID
            try {
                println(s"executing $name...")
                if (ownFreeWill)
                    taskAchiever.sendTaskInfo()
                taskAchiever.execute()
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }

        override def toString: String = s"Ticket(name = $name," +
                s" ownerID = $ownerID," +
                s" id = $sessionID," +
                s" freeWill = $ownFreeWill)"

    }

}