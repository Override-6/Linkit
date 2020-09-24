package fr.overridescala.vps.ftp.client

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.collection.mutable

class ClientTaskCompleterHandler(private val tasksHandler: TasksHandler)
        extends TaskCompleterHandler {

    private lazy val completers: mutable.Map[String, (DataPacket, TasksHandler, String) => Unit] =
        new mutable.HashMap[String, (DataPacket, TasksHandler, String) => Unit]()

    override def handleCompleter(initPacket: DataPacket, ownerID: String): Unit = {
        val taskType = initPacket.header
        val content = initPacket.content
        val contentString = new String(content)
        val taskID = initPacket.taskID
        taskType match {
            case UploadTask.UPLOAD =>
                new DownloadTask(tasksHandler, Utils.deserialize(content))
                        .queue()
            case DownloadTask.DOWNLOAD =>
                new UploadTask(tasksHandler, Utils.deserialize(content))
                        .queue()
            case FileInfoTask.FILE_INFO =>
                val task = new FileInfoTask.FileInfoCompleter(contentString)
                tasksHandler.registerTask(task, taskID, false, ownerID)
            case CreateFileTask.CREATE_FILE =>
                val task = new CreateFileTask.CreateFileCompleter(contentString)
                tasksHandler.registerTask(task, taskID, false, ownerID)
            case "STRSS" =>
                val task = new StressTestTask.StressTestCompleter(contentString.toLong)
                tasksHandler.registerTask(task, taskID, false, ownerID)
            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier(initPacket, tasksHandler, ownerID)
        }
    }

    override def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler, String) => Unit): Unit =
        completers.put(taskType, supplier)

}