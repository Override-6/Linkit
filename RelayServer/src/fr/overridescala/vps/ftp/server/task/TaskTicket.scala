package fr.overridescala.vps.ftp.server.task

import java.io.IOException
import java.net.Socket

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.task.TaskExecutor

class TaskTicket(private val executor: TaskExecutor,
                 private val taskID: Int,
                 private val targetID: String,
                 private val senderIdentifier: String,
                 private val socket: Socket,
                 private val packetManager: PacketManager,
                 private val ownFreeWill: Boolean) {

    var channel: SimplePacketChannel = new SimplePacketChannel(socket, targetID, senderIdentifier, packetManager, taskID)
    val taskName: String = executor.getClass.getSimpleName

    def notifyExecutor(): Unit = executor.synchronized {
        executor.notifyAll()
    }


    def start(): Unit = {
        try {
            println(s"executing $taskName...")
            if (ownFreeWill) {
                channel.sendInitPacket(executor.initInfo)
            }
            executor.execute(channel)
            notifyExecutor()
            println(s"$taskName completed !")
        } catch {
            // Do not prints those exceptions : they are normal errors
            // lifted when a task is brutally aborted via Thread.stop
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
