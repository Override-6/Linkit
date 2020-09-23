package fr.overridescala.vps.ftp.client

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.collection.mutable

class ClientTaskCompleterHandler(private val tasksHandler: TasksHandler)
        extends TaskCompleterHandler {

    private lazy val completers: mutable.Map[String, (DataPacket, TasksHandler) => TaskExecutor] = new mutable.HashMap[String, (DataPacket, TasksHandler) => TaskExecutor]()

    override def handleCompleter(initPacket: DataPacket, ownerID: String): Unit = {
        val taskType = initPacket.header
        val content = initPacket.content
        val contentString = new String(content)
        taskType match {
            case UploadTask.UPLOAD =>
                new DownloadTask(tasksHandler, Utils.deserialize(content))
            case DownloadTask.DOWNLOAD =>
                new UploadTask(tasksHandler, Utils.deserialize(content))
            case FileInfoTask.FILE_INFO =>
                new FileInfoTask.FileInfoCompleter(contentString)
            case CreateFileTask.CREATE_FILE =>
                new CreateFileTask.CreateFileCompleter(contentString)
            case "STRSS" =>
                new StressTestTask.StressTestCompleter(contentString.toLong)

            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier(initPacket, tasksHandler)
        }
    }

    override def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler) => TaskExecutor): Unit =
        completers.put(taskType, supplier)
}
