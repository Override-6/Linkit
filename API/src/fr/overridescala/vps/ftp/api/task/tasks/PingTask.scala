package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.PingTask.{PING, TYPE}
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TaskInitInfo, TasksHandler}

class PingTask(private val handler: TasksHandler,
               private val targetID: String) extends Task[Long](handler, targetID) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetID)

    override def execute(channel: PacketChannel): Unit = {
        val t0 = System.currentTimeMillis()
        channel.sendPacket(PING)
        channel.nextPacket()
        val t1 = System.currentTimeMillis()
        success(t1 - t0)
    }
}

object PingTask {
    private val PING = "PING"
    val TYPE: String = PING

    class PingCompleter extends TaskExecutor {
        override def execute(channel: PacketChannel): Unit = {
            val t0 = System.currentTimeMillis()
            channel.sendPacket("OK")
            val t01 = System.currentTimeMillis()
            println(s"time to send the packet : ${t01 - t0}")
        }
    }

    def concoct(targetID: String): TaskConcoctor[Long] =
        handler => new PingTask(handler, targetID)

}
