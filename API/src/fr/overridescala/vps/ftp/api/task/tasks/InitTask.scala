package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}

class InitTask(private val handler: TasksHandler,
               private val channel: PacketChannel,
               private val id: String)
        extends Task[Unit](handler, channel.ownerAddress) with TaskExecutor {


    override def sendTaskInfo(): Unit = {
        channel.sendPacket("INIT", id.getBytes)
        channel.sendPacket("1", "nieu nieu nieu ceci est un packet de test pour comprendre pk mon vps bug quand il commence une task download ca kasse lékouys".getBytes)
    }

    override def execute(): Unit = {
        val notAccepted = channel.nextPacket().header.equals("ERROR")
        if (notAccepted)
            error("no id where available.")
        else success(id)
    }

}
