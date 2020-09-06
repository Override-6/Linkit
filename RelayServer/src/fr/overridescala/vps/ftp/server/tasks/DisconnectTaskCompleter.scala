package fr.overridescala.vps.ftp.server.tasks

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.task.{TaskAchiever, TaskType}
import fr.overridescala.vps.ftp.server.RelayServer

class DisconnectTaskCompleter(server: RelayServer,
                              address: InetSocketAddress) extends TaskAchiever {

    override val taskType: TaskType = TaskType.DISCONNECT

    override def achieve(): Unit = {
        server.disconnect(address)
    }
}
