package fr.overridescala.vps.ftp.server.tasks

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{TaskAchiever, TaskType}
import fr.overridescala.vps.ftp.server.RelayServer

class InitTaskCompleter(private var server: RelayServer,
                        private var channel: PacketChannel,
                        private var id: String) extends TaskAchiever {

    override val taskType: TaskType = TaskType.INITIALISATION

    override def achieve(): Unit = {
        val success = server.attributeID(channel.getOwnerAddress, id)
        if (!success)
            channel.sendPacket(new TaskPacket(taskType, "ERROR"))
    }
}
