package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.PingTask.PING
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}

class PingTask(private val handler: TasksHandler,
               private val targetID: String) extends Task[Long](handler, targetID) {

    override def execute(channel: PacketChannel): Unit = {
        val t0 = System.currentTimeMillis()
        channel.sendPacket(PING)
        channel.nextPacket()
        val t1 = System.currentTimeMillis()
        success(t1 - t0)
    }


}

object PingTask {
    val PING = "PING"

    class PingCompleter extends TaskExecutor {
        override def execute(channel: PacketChannel): Unit =
            channel.sendPacket("OK")
    }

    def concoct(targetID: String): TaskConcoctor[Long] = handler => {
        new PingTask(handler, targetID)
    }
}
