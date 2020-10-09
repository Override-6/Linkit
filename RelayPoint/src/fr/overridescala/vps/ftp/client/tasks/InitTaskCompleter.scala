package fr.overridescala.vps.ftp.client.tasks

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.ErrorPacket
import fr.overridescala.vps.ftp.api.task.TaskExecutor

class InitTaskCompleter(private val relay: Relay) extends TaskExecutor {

    override def execute(channel: PacketChannel): Unit = {
        val identifier = relay.identifier
        channel.sendPacket(identifier)
        channel.nextPacket() match {
            case _: ErrorPacket =>
                throw RelayInitialisationException(s"another relay point with id '$identifier' is currently connected on the targeted network")
            case _ => println("successfully connected to the server !")
        }
    }
}

object InitTaskCompleter {
    val TYPE = "GID"
}
