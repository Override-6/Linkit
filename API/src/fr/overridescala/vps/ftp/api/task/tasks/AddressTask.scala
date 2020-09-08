package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Constants

class AddressTask(private val channel: PacketChannel,
                  private val handler: TasksHandler,
                  private val id: String)
        extends Task[InetSocketAddress](handler, channel.ownerAddress) with TaskAchiever {

    override def preAchieve(): Unit = channel.sendPacket(id)

    override def achieve(): Unit = {
        val response = channel.nextPacket()
        if (response.header.equals("ERROR")) {
            error(new String(response.content))
            return
        }
        success(new InetSocketAddress(new String(response.content), Constants.PORT))
    }


}
