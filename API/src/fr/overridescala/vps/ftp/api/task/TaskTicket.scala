package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel

class TaskTicket(private val taskExecutor: TaskExecutor,
                 val sessionID: Int,
                 private val socket: SocketChannel,
                 private val ownerID: String,
                 private val ownFreeWill: Boolean) {

    val taskName: String = taskExecutor.getClass.getSimpleName

    def start(): Unit = {
        try {
            println(s"executing $taskName...")
            val channel = new SimplePacketChannel(socket, ownerID, sessionID)
            if (ownFreeWill)
                taskExecutor.sendTaskInfo(channel)
            taskExecutor.execute(channel)
        } catch {
            case e: Throwable => e.printStackTrace()
        }
    }

    override def toString: String =
        s"Ticket(name = $taskName," +
                s" id = $sessionID," +
                s" freeWill = $ownFreeWill)"

}
