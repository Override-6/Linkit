package fr.overridescala.vps.ftp.server.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.TaskAchiever
import fr.overridescala.vps.ftp.server.RelayServer

class AddressTaskCompleter(private val packetChannel: PacketChannel,
                           private val server: RelayServer,
                           private val id: String)
        extends TaskAchiever {

    override def achieve(): Unit = {
        val address = server.getAddress(id)
        if (address == null) {
            val errorMsg = "unable to retrieve the associated address from identifier"
            packetChannel.sendPacket("ERROR", errorMsg.getBytes)
            return
        }
        packetChannel.sendPacket("OK", address.toString.getBytes)
    }
}
