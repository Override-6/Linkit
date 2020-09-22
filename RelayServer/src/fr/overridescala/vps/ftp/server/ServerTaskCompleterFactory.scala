package fr.overridescala.vps.ftp.server

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterFactory, TaskExecutor}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.collection.mutable

class ServerTaskCompleterFactory(private val tasksHandler: ServerTasksHandler) extends TaskCompleterFactory {

    private lazy val completers: mutable.Map[String, DataPacket => TaskExecutor] = new mutable.HashMap[String, DataPacket => TaskExecutor]()

    override def getCompleter(initPacket: DataPacket): TaskExecutor = {
        val taskType = initPacket.header
        val content = initPacket.content
        val contentString = new String(content)
        taskType match {
            case "STRSS" =>
                new StressTestTask.StressTestCompleter(contentString.toLong)
            case UploadTask.UPLOAD =>
                new DownloadTask(tasksHandler, Utils.deserialize(content))
            case DownloadTask.DOWNLOAD =>
                new UploadTask(tasksHandler, Utils.deserialize(content))
            case FileInfoTask.FILE_INFO =>
                new FileInfoTask.FileInfoCompleter(Utils.deserialize(content).asInstanceOf[(String, _)]._1)
            case CreateFileTask.CREATE_FILE =>
                new CreateFileTask.CreateFileCompleter(contentString)

            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier.apply(initPacket)
        }
    }

    override def putCompleter(completerType: String, supplier: DataPacket => TaskExecutor): Unit =
        completers.put(completerType, supplier)
}
