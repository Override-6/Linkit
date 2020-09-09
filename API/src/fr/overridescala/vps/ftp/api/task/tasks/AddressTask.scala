package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Constants

class AddressTask(private val channel: PacketChannel,
                  private val handler: TasksHandler,
                  private val id: String)
        extends Task[InetSocketAddress](handler, channel.ownerAddress) with TaskExecutor {

    override def getInitPacket(): Unit = channel.sendPacket("ADR", id.getBytes)

    override def execute(): Unit = {
        val response = channel.nextPacket()
        if (response.header.equals("ERROR")) {
            error(new String(response.content))
            return
        }
        success(new InetSocketAddress(new String(response.content), Constants.PORT))
    }


}
