package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel

class TaskTicket(private val taskExecutor: TaskExecutor,
                 private val currentTaskID: Int,
                 private val socket: SocketChannel,
                 private val ownerID: String,
                 private val ownFreeWill: Boolean) {

    val channel = new SimplePacketChannel(socket, ownerID, currentTaskID)
    val taskName: String = taskExecutor.getClass.getSimpleName

    def start(): Unit = {
        try {
            println(s"executing $taskName...")
            if (ownFreeWill)
                taskExecutor.sendTaskInfo(channel)
            taskExecutor.execute(channel)
        } catch {
            case e: Throwable => e.printStackTrace()
        }
    }

    override def toString: String =
        s"Ticket(name = $taskName," +
                s" id = $currentTaskID," +
                s" freeWill = $ownFreeWill)"

}
