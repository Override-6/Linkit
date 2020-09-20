package fr.overridescala.vps.ftp.client

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{CreateFileTask, DownloadTask, FileInfoTask, StressTestTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{DynamicTaskCompleterFactory, TaskExecutor}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.collection.mutable

class RelayPointTaskCompleterFactory(private val tasksHandler: ServerTasksHandler)
        extends DynamicTaskCompleterFactory {

    private lazy val completers: mutable.Map[String, DataPacket => TaskExecutor] = new mutable.HashMap[String, DataPacket => TaskExecutor]()

    override def getCompleter(initPacket: DataPacket): TaskExecutor = {
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
                completerSupplier(initPacket)
        }
    }

    override def putCompleter(completerType: String, supplier: DataPacket => TaskExecutor): Unit =
        completers.put(completerType, supplier)
}
