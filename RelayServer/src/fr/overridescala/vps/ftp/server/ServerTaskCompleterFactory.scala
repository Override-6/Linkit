package fr.overridescala.vps.ftp.server

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, DataPacket}
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{TaskAchiever, TaskCompleterFactory, TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.tasks.{AddressTaskCompleter, InitTaskCompleter}

class ServerTaskCompleterFactory(private val tasksHandler: TasksHandler,
                                 private val server: RelayServer) extends TaskCompleterFactory {

    override def getCompleter(channel: PacketChannel, initPacket: DataPacket): TaskAchiever = {
        val taskType = initPacket.header
        val content = initPacket.content
        taskType match {
            case "UP" => new DownloadTask(channel, tasksHandler, Utils.deserialize(content))
            case "DWN" => new UploadTask(channel, tasksHandler, Utils.deserialize(content))
            case "FINFO" => new FileInfoTask.Completer(channel, new String(content))
            case "ADR" => new AddressTaskCompleter(channel, server, new String(content))
            case "INIT" => new InitTaskCompleter(server, channel, new String(content))
            case _ => throw new IllegalArgumentException("could not find completer for task " + taskType)
        }
    }
}
