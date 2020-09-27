package fr.overridescala.vps.ftp.server.task

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.DataPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.task.ServerTaskCompleterHandler.TempFolder

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServerTaskCompleterHandler(private val tasksHandler: ServerTasksHandler,
                                 private val server: Relay) extends TaskCompleterHandler {

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

            case PingTask.PING =>
                new PingTask.PingCompleter

            case FileInfoTask.FILE_INFO =>
                val pair: (String, _) = Utils.deserialize(content)
                new FileInfoTask.FileInfoCompleter(pair._1)

            case CreateFileTask.CREATE_FILE =>
                new CreateFileTask.CreateFileCompleter(new String(content.slice(1, content.length)), content(0) == 1)

            case "STRSS" =>
                new StressTestTask.StressTestCompleter(contentString.toLong)

            case _ =>
        }
    }

    override def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler, String) => Unit): Unit =
        completers.put(taskType, supplier)

    private def handleUpload(uploadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        val desc = TransferDescription.builder()
                .setSource(uploadDesc.source)
                .setDestination(uploadDesc.destination)
                .setTargetID(ownerID)
                .build()
        if (!uploadDesc.targetID.equals(server.identifier)) {
            val redirectedTransferDesc = TransferDescription.builder()
                    .setTargetID(uploadDesc.targetID)
                    .setDestination(TempFolder)
                    .setSource(uploadDesc.source)
                    .build()
            new DownloadTask(tasksHandler, desc).queue(_ => {
                new UploadTask(tasksHandler, redirectedTransferDesc)
            }, _, taskID)
            return
        }

        new DownloadTask(tasksHandler, desc).queue(_, _, taskID)
    }

    private def handleDownload(downloadDesc: TransferDescription, ownerID: String, taskID: Int): Unit = {
        val desc = TransferDescription.builder()
                .setSource(downloadDesc.source)
                .setDestination(downloadDesc.destination)
                .setTargetID(ownerID)
                .build()
        if (!downloadDesc.targetID.equals(server.identifier)) {
            val redirectedTransferDesc = TransferDescription.builder()
                    .setTargetID(downloadDesc.targetID)
                    .setDestination(TempFolder)
                    .setSource(downloadDesc.source)
                    .build()
            new DownloadTask(tasksHandler, redirectedTransferDesc).queue(_ => {
                new UploadTask(tasksHandler, desc)
            }, _, taskID)
            return
        }
        new UploadTask(tasksHandler, desc).queue(_, _, taskID)
    }

    private def testTransfer(packet: DataPacket, senderId: String): Boolean = {
        val taskType = packet.header
        val taskID = packet.taskID
        val content = packet.content
        taskType match {
            case UploadTask.UPLOAD =>
                handleUpload(Utils.deserialize(content), senderId, taskID)
                true

            case DownloadTask.DOWNLOAD =>
                handleDownload(Utils.deserialize(content), senderId, taskID)
                true
            case _ => false
        }
    }

    private def testOther(packet: DataPacket, senderId: String): Boolean = {
        val taskType = packet.header
        val content = packet.content
        val contentString = new String(content)
        val pairTaskAndTarget = taskType match {
            case PingTask.PING =>
                new PingTask.PingCompleter

            case FileInfoTask.FILE_INFO =>
                val pair: (String, _) = Utils.deserialize(content)
                new FileInfoTask.FileInfoCompleter(pair._1)

            case CreateFileTask.CREATE_FILE =>
                new CreateFileTask.CreateFileCompleter(new String(content.slice(1, content.length)), content(0) == 1)

            case "STRSS" =>
                new StressTestTask.StressTestCompleter(contentString.toLong)
        }
        val taskID = packet.taskID
        tasksHandler.registerTask(task, taskID, false, )
    }

}

object ServerTaskCompleterHandler {
    val TempFolder = "/home/override/VPS/FileTransferer/Temp"
}
