package fr.overridescala.vps.ftp.client.tasks

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.TaskExecutor

class InitTaskCompleter(private val identifier: String) extends TaskExecutor {

    override def execute(channel: PacketChannel): Unit = {
        channel.sendPacket(identifier)
        val response = channel.nextPacket().header
        println(response)
        if (response.equals("ERROR"))
            throw RelayInitialisationException(response)
    }
}

object InitTaskCompleter {
    val INIT = "GID"
}
