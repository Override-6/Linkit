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

    private lazy val completers: mutable.Map[String, (DataPacket, TasksHandler, String) => TaskExecutor]
    = new mutable.HashMap[String, (DataPacket, TasksHandler, String) => TaskExecutor]()

    override def handleCompleter(initPacket: DataPacket, senderId: String): Unit = {
        val taskType = initPacket.header
        val taskIdentifier = initPacket.taskID
        val content = initPacket.content
        val contentString = new String(content)
        taskType match {
            case UploadTask.UPLOAD =>
                handleUpload(Utils.deserialize(content), senderId)
            case DownloadTask.DOWNLOAD =>
                handleDownload(Utils.deserialize(content), senderId)
            case FileInfoTask.FILE_INFO =>
                val pair: (String, String) = Utils.deserialize(content)
                val completer = new FileInfoTask.FileInfoCompleter(pair._1)
                tasksHandler.registerTask(completer, taskIdentifier, true, pair._2, senderId)
                //TODO handle CreateFileTask and StressTask
            /*case CreateFileTask.CREATE_FILE =>
                val completer = new CreateFileTask.CreateFileCompleter(contentString)
                tasksHandler.registerTask(completer, taskIdentifier, senderId, false)
            case "STRSS" =>
                val completer = new StressTestTask.StressTestCompleter(contentString.toLong)
                tasksHandler.registerTask(completer, taskIdentifier, senderId, false)
             */

            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier.apply(initPacket, tasksHandler, senderId)
        }
    }

    override def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler, String) => TaskExecutor): Unit =
        completers.put(taskType, supplier)

    def handleUpload(uploadDesc: TransferDescription, ownerID: String): Unit = {
        var onCompleted: Unit => Unit = null
        var desc = uploadDesc
        if (!uploadDesc.targetID.equals(server.identifier)) {
            desc = TransferDescription.builder()
                    .setTargetID(uploadDesc.targetID)
                    .setSource(uploadDesc.source)
                    .setDestination(TempFolder)
                    .build()
            onCompleted = _ => {
                val uploadToTarget = TransferDescription.builder()
                        .setTargetID(ownerID)
                        .setSource(FileDescription.fromLocal(TempFolder))
                        .setDestination(uploadDesc.destination)
                        .build()
                new UploadTask(tasksHandler, uploadToTarget).queue(end, end)

                def end(any: Any): Unit = Files.deleteIfExists(Utils.formatPath(TempFolder))
            }
        }

        new DownloadTask(tasksHandler, desc)
                .queue(onCompleted)
    }

    def handleDownload(downloadDesc: TransferDescription, ownerID: String): Unit = {
        var onCompleted: Unit => Unit = null
        var desc: TransferDescription = null
        if (!downloadDesc.targetID.equals(server.identifier)) {
            desc = TransferDescription.builder()
                    .setTargetID(downloadDesc.targetID)
                    .setSource(downloadDesc.source)
                    .setDestination(TempFolder)
                    .build()
            onCompleted = _ => {
                val uploadToOwner = TransferDescription.builder()
                        .setTargetID(ownerID)
                        .setSource(FileDescription.fromLocal(TempFolder))
                        .setDestination(downloadDesc.destination)
                        .build()
                new UploadTask(tasksHandler, uploadToOwner).queue(_ =>
                    Files.deleteIfExists(Utils.formatPath(TempFolder)))
            }
        } else desc = TransferDescription.builder()
                .setTargetID(ownerID)
                .setSource(downloadDesc.source)
                .setDestination(downloadDesc.destination)
                .build()


        new DownloadTask(tasksHandler, desc)
                .queue(onCompleted)
    }


}

object ServerTaskCompleterHandler {
    val TempFolder = "/home/override/VPS/FileTransferer/Temp"
}
