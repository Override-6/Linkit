package fr.overridescala.vps.ftp.client

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.{TaskCompleterFactory, TaskExecutor, TasksHandler}

import scala.collection.mutable

class ClientTasksHandler(socket: SocketChannel) extends TasksHandler{

    private val queue: mutable.Queue[TaskAchieverTicket] = mutable.Queue.empty
    private var currentSessionID = -1

    override def registerTask(achiever: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean): Unit = {
        val ticket = new TaskAchieverTicket(achiever, ownerID, sessionID, ownFreeWill)
        queue.addOne(ticket)
        println()
        synchronized(notifyAll())
    }

    def start(): Unit = {
        val thread = new Thread(() => {
            while (true) {
                queue.dequeue().start()
                println("waiting for another task to complete...")
            }
        })
        thread.setName("Tasks")
        thread.start()
    }

    override def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, ownerID: String, socket: SocketChannel): Unit = {
        if (packet.taskID != currentSessionID) {
            val completer = factory.getCompleter(packet)
            registerTask(completer, packet.taskID, ownerID, false)
            return
        }
        channel.addPacket(packet)
    }

    private class TaskAchieverTicket(private val taskAchiever: TaskExecutor,
                                     private val ownerID: String,
                                     private val sessionID: Int,
                                     private val ownFreeWill: Boolean) {

        val name: String = taskAchiever.getClass.getSimpleName

        def start(): Unit = {
            currentTaskOwner = ownerID
            currentSessionID = sessionID
            try {
                println(s"executing $name...")
                channel = new
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
