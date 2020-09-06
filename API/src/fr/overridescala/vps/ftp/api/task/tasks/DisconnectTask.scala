package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}

class DisconnectTask(private val tasksHandler: TasksHandler,
                     private val channel: PacketChannel)
        extends Task[Unit] with TaskAchiever {

    override def enqueue(): Unit = tasksHandler.register(this, channel.getOwnerAddress, true)

    override val taskType: TaskType = TaskType.DISCONNECT

    override def achieve(): Unit = {
        channel.sendPacket(new TaskPacket(taskType, ""))
        success()
    }
}
