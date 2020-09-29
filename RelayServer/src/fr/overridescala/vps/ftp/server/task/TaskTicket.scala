package fr.overridescala.vps.ftp.server.task

import java.io.IOException
import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
import fr.overridescala.vps.ftp.api.task.TaskExecutor

class TaskTicket(private val executor: TaskExecutor,
                 private val taskID: Int,
                 private val socket: SocketChannel,
                 private val ownFreeWill: Boolean) {

    val channel = new SimplePacketChannel(socket, taskID)
    val taskName: String = executor.getClass.getSimpleName

    def start(): Unit = {
        try {
            println(s"executing $taskName...")
            if (ownFreeWill)
                channel.sendInitPacket(executor.initInfo)
            executor.execute(channel)
            println(s"$taskName completed !")
        } catch {
            // do not prints those exceptions : normal errors when a task is brutally aborted via Thread.stop
            case _: IllegalMonitorStateException =>
            case _: InterruptedException =>
            case e: IOException if e.getMessage != null && e.getMessage.equalsIgnoreCase("Broken pipe") =>

            case e: Throwable => e.printStackTrace()
        }
    }

    override def toString: String =
        s"Ticket(taskName = $taskName," +
                s" taskID = $taskID," +
                s" freeWill = $ownFreeWill)"

}
