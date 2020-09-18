package fr.overridescala.vps.ftp.server

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{DynamicTaskCompleterFactory, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.tasks.InitTaskCompleter

import scala.collection.mutable

class ServerTaskCompleterFactory(private val tasksHandler: TasksHandler,
                                 private val server: RelayServer) extends DynamicTaskCompleterFactory {

    private lazy val completers: mutable.Map[String, DataPacket => TaskExecutor] = new mutable.HashMap[String, DataPacket => TaskExecutor]()

    override def getCompleter(channel: PacketChannel, initPacket: DataPacket): TaskExecutor = {
        val taskType = initPacket.header
        val content = initPacket.content
        val contentString = new String(content)
        taskType match {
            case "STRSS" =>
                new StressTestTask.StressTestCompleter(channel, contentString.toLong)
            case UploadTask.UPLOAD =>
                new DownloadTask(channel, tasksHandler, Utils.deserialize(content))
            case DownloadTask.DOWNLOAD =>
                new UploadTask(channel, tasksHandler, Utils.deserialize(content))
            case FileInfoTask.FILE_INFO =>
                new FileInfoTask.FileInfoCompleter(channel, Utils.deserialize(content).asInstanceOf[(String, _)]._1)
            case InitTask.INIT =>
                new InitTaskCompleter(server, channel, contentString)
            case CreateFileTask.CREATE_FILE =>
                new CreateFileTask.CreateFileCompleter(channel, contentString)

            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier.apply(initPacket)
        }
    }

    override def putCompleter(completerType: String, supplier: DataPacket => TaskExecutor): Unit =
        completers.put(completerType, supplier)
}
