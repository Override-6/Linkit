package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.{Constants, Protocol}

class AddressTask(private val channel: PacketChannel,
                  private val queue: TasksHandler,
                  private val id: String)
        extends Task[InetSocketAddress]() with TaskAchiever {

    override val taskType: TaskType = TaskType.ADDRESS

    override def enqueue(): Unit = queue.register(this, channel.getOwnerAddress, true)

    override def achieve(): Unit = {
        channel.sendPacket(new TaskPacket(taskType, id))
        val response = channel.nextPacket()
        if (response.header.equals("ERROR")){
            error(new String(response.content))
            return
        }
        success(new InetSocketAddress(new String(response.content), Constants.PORT))
    }


}
