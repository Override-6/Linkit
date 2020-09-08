package fr.overridescala.vps.ftp.server.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.TaskAchiever
import fr.overridescala.vps.ftp.server.RelayServer

class InitTaskCompleter(private var server: RelayServer,
                        private var channel: PacketChannel,
                        private var id: String) extends TaskAchiever {

    override def achieve(): Unit = {
        val success = server.attributeID(channel.ownerAddress, id)
        val response = if (success) "OK" else "ERROR"
        channel.sendPacket(response)
    }
}
