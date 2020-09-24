package fr.overridescala.vps.ftp.server

import java.nio.file.Files

import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.ServerTaskCompleterHandler.TempFolder

import scala.collection.mutable

class ServerTaskCompleterHandler(private val tasksHandler: ServerTasksHandler,
                                 private val server: RelayServer) extends TaskCompleterHandler {

    private lazy val completers: mutable.Map[String, (DataPacket, TasksHandler, String) => Unit]
    = new mutable.HashMap[String, (DataPacket, TasksHandler, String) => Unit]()

    override def handleCompleter(initPacket: DataPacket, senderId: String): Unit = {
        val taskType = initPacket.header
        val taskID = initPacket.taskID
        val content = initPacket.content
        val contentString = new String(content)
        taskType match {
            case UploadTask.UPLOAD =>
                handleUpload(Utils.deserialize(content), senderId, taskID)
            case DownloadTask.DOWNLOAD =>
                handleDownload(Utils.deserialize(content), senderId, taskID)
            case FileInfoTask.FILE_INFO =>
                val pair: (String, String) = Utils.deserialize(content)
                val completer = new FileInfoTask.FileInfoCompleter(pair._1)
                tasksHandler.registerTask(completer, taskID, true, pair._2, senderId)
            case CreateFileTask.CREATE_FILE =>
                val completer = new CreateFileTask.CreateFileCompleter(contentString)
                tasksHandler.registerTask(completer, taskID, false, senderId)
            case "STRSS" =>
                val completer = new StressTestTask.StressTestCompleter(contentString.toLong)
                tasksHandler.registerTask(completer, taskID, false, senderId)


            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier.apply(initPacket, tasksHandler, senderId)
        }
    }

    override def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler, String) => Unit): Unit =
        completers.put(taskType, supplier)

    def handleUpload(uploadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        if (!uploadDesc.targetID.equals(server.identifier)) {
            redirectUpload(uploadDesc, ownerID, taskID)
            return
        }

        val task = new DownloadTask(tasksHandler, uploadDesc)
        tasksHandler.registerTask(task, taskID, false, ownerID)
    }

    def handleDownload(downloadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        if (!downloadDesc.targetID.equals(server.identifier)) {
            redirectDownload(downloadDesc, ownerID, taskID)
            return
        }
        val desc = TransferDescription.builder()
                .setSource(downloadDesc.source)
                .setDestination(downloadDesc.destination)
                .setTargetID(ownerID)
                .build()
        val task = new UploadTask(tasksHandler, desc)
        tasksHandler.registerTask(task, taskID, false, ownerID)
    }

    def redirectDownload(downloadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        val desc = TransferDescription.builder()
                .setSource(downloadDesc.source)
                .setDestination(downloadDesc.destination)
                .setTargetID(ownerID)
                .build()
        val task = new DownloadTask(tasksHandler, desc)
        tasksHandler.registerTask(task, taskID, false, ownerID)
    }

    def redirectUpload(uploadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        val desc = TransferDescription.builder()
                .setTargetID(uploadDesc.targetID)
                .setSource(uploadDesc.source)
                .setDestination(TempFolder)
                .build()
        val task = new DownloadTask(tasksHandler, desc)
        tasksHandler.registerTask(task, taskID, false, ownerID)
    }


}

object ServerTaskCompleterHandler {
    val TempFolder = "/home/override/VPS/FileTransferer/Temp"
}
