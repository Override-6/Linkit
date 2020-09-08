package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}

class DisconnectTask(private val handler: TasksHandler,
                     private val channel: PacketChannel)
        extends Task[Unit](handler, channel.ownerAddress) with TaskAchiever {

    override val taskType: TaskType = TaskType.DISCONNECT

    override def achieve(): Unit = {
        channel.sendPacket(taskType, "")
        success()
    }
}
