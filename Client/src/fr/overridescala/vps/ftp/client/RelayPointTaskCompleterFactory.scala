package fr.overridescala.vps.ftp.client

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{TaskAchiever, TaskCompleterFactory, TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils

class RelayPointTaskCompleterFactory(private val tasksHandler: TasksHandler)
        extends TaskCompleterFactory {

    override def getCompleter(channel: PacketChannel, taskType: TaskType, header: String, content: Array[Byte]): TaskAchiever = {
        taskType match {
            case TaskType.UPLOAD => new DownloadTask(channel, tasksHandler, Utils.deserialize(content))
            case TaskType.DOWNLOAD => new UploadTask(channel, tasksHandler, Utils.deserialize(content))
            case TaskType.FILE_INFO => new FileInfoTask.Completer(channel, header)
            case _ => throw new IllegalArgumentException("could not find completer for task " + taskType)
        }
    }
}
