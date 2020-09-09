package fr.overridescala.vps.ftp.api.task

import java.net.SocketAddress
import java.util

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, SimplePacketChannel, DataPacket}


class TasksHandler() {

    private val queue: util.Queue[TaskAchieverTicket] = new util.ArrayDeque[TaskAchieverTicket]()
    var currentSessionID: Int = -1
    private var currentTaskOwner: SocketAddress = _


    def register(achiever: TaskExecutor, sessionID: Int, owner: SocketAddress, ownFreeWill: Boolean): Unit = {
        queue.offer(new TaskAchieverTicket(achiever, owner, sessionID, ownFreeWill))
        synchronized(notifyAll())
    }

    def start(): Unit = {
        new Thread(() => {
            while (true) {
                startNextTask()
                println("waiting for another task to complete...")
            }
        }).start()
    }

    def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, channel: SimplePacketChannel): Unit = {
        if (packet.sessionID != currentSessionID) {
            val completer = factory.getCompleter(channel, packet)
            register(completer, packet.sessionID, channel.ownerAddress, false)
            return
        }
        channel.addPacket(packet)
    }

    def cancelTasks(address: SocketAddress): Unit = {
        if (currentTaskOwner != null && currentTaskOwner.equals(address)) {
            synchronized(notifyAll())
            currentTaskOwner = null
        }
        queue.removeIf(_.isOwner(address))
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
                                     val owner: SocketAddress,
                                     val sessionID: Int,
                                     val ownFreeWill: Boolean) {

        def isOwner(address: SocketAddress): Boolean = address.equals(owner)

        def start(): Unit = {
            currentTaskOwner = owner
            currentSessionID = sessionID
            if (ownFreeWill)
                taskAchiever.getInitPacket()
            try {
                taskAchiever.execute()
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }
    }

}