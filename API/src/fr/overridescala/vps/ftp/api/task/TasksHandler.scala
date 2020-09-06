package fr.overridescala.vps.ftp.api.task

import java.net.SocketAddress
import java.util

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}


class TasksHandler() {
    private val queue: util.Queue[TaskAchieverTicket] = new util.ArrayDeque[TaskAchieverTicket]()


    private var currentTaskType: TaskType = _
    private var currentTaskOwner: SocketAddress = _


    def register(achiever: TaskAchiever, owner: SocketAddress, ownFreeWill: Boolean): Unit = {
        queue.offer(new TaskAchieverTicket(achiever, owner, ownFreeWill))
        synchronized (notifyAll())
    }

    def start(): Unit = {
        new Thread(() => {
            while (true) {
                startNextTask()
                println("waiting for another task to complete...")
            }
        }).start()
    }

    /**
     * determines if this packet is a new task request
     *
     * @return true if a new Task where created, false instead
     **/
    def handlePacket(packet: TaskPacket, factory: TaskCompleterFactory, packetChannel: PacketChannel): Boolean = {
        val task = packet.taskType
        if (currentTaskType == null) {
            registerCompleter()
            return true
        }
        if (currentTaskType != task) {
            if (task.isCompleterOf(currentTaskType))
                return false
            registerCompleter()
            return true
        }

        def registerCompleter(): Unit = {
            val completer = factory.getCompleter(packetChannel, packet.taskType, packet.header, packet.content)
            register(completer, packetChannel.getOwnerAddress, false)
        }

        false
    }

    def cancelTasks(address: SocketAddress): Unit = {
        if (currentTaskOwner != null && currentTaskOwner.equals(address)) {
            synchronized (notifyAll())
            currentTaskOwner = null
            currentTaskType = null
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

    private class TaskAchieverTicket(val taskAchiever: TaskAchiever,
                                     val owner: SocketAddress,
                                     val ownFreeWill: Boolean) {

        def isOwner(address: SocketAddress): Boolean = address.equals(owner)

        def start(): Unit = {
            if (ownFreeWill)
                taskAchiever.preAchieve()
            currentTaskType = taskAchiever.taskType
            currentTaskOwner = owner
            taskAchiever.achieve()
        }
    }

}