package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}

class InitTask(private val handler: TasksHandler,
               private val channel: PacketChannel,
               private val id: String)
        extends Task[Unit](handler, channel.ownerAddress) with TaskAchiever {


    override val taskType: TaskType = TaskType.INITIALISATION

    override def preAchieve(): Unit = {
        channel.sendPacket(taskType, id)
    }

    override def achieve(): Unit = {
        val notAccepted = channel.nextPacket().header.equals("ERROR")
        if (notAccepted)
            error("no id where available.")
        else success(id)
    }

}
