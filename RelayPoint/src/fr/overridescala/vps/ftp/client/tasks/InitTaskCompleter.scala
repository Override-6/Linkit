package fr.overridescala.vps.ftp.client.tasks

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.task.TaskExecutor

class InitTaskCompleter(private val relay: Relay) extends TaskExecutor {

    override def execute(): Unit = {
        val identifier = relay.identifier
        channel.sendPacket(DataPacket(identifier))
        val response: DataPacket = channel.nextPacketAsP()
        
        if (response.header == "ERROR")
            throw RelayInitialisationException(s"another relay point with id '$identifier' is currently connected on the targeted network")
        println("successfully connected to the server !")

    }
}

object InitTaskCompleter {
    val TYPE = "GID"
}
