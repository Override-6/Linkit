package fr.overridescala.vps.ftp.server.task

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.TaskInitPacket
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferDescriptionBuilder}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.task.ServerTaskCompleterHandler.TempFolder

import scala.collection.mutable

class ServerTaskCompleterHandler(private val server: Relay) extends TaskCompleterHandler {

    private lazy val completers: mutable.Map[String, (TaskInitPacket, TasksHandler, String) => Unit]
    = new mutable.HashMap[String, (TaskInitPacket, TasksHandler, String) => Unit]()

    override def handleCompleter(initPacket: TaskInitPacket, senderId: String, handler: TasksHandler): Unit =
        if (testTransfer(initPacket, senderId, handler) && testOther(initPacket, handler))
            testMap(initPacket, senderId, handler)

    private def testTransfer(packet: TaskInitPacket, senderId: String, handler: TasksHandler): Boolean = {
        val taskType = packet.taskType
        val taskID = packet.taskID
        val content = packet.content
        //println("bytesArray = " + content.mkString("Array(", ", ", ")"))
        taskType match {
            case UploadTask.TYPE =>
                handleUpload(Utils.deserialize(content), senderId, taskID, handler)
                false
            case DownloadTask.TYPE =>
                handleDownload(Utils.deserialize(content), senderId, taskID, handler)
                false

            case _ => true
        }
    }

    private def testOther(packet: TaskInitPacket, handler: TasksHandler): Boolean = {
        val taskType = packet.taskType
        val content = packet.content
        val contentString = new String(content)
        val task = taskType match {
            case FileInfoTask.TYPE => new FileInfoTask.FileInfoCompleter(contentString)
            case CreateFileTask.TYPE => new CreateFileTask.CreateFileCompleter(contentString.slice(1, content.length), content(0) == 1)
            case PingTask.TYPE => new PingTask.PingCompleter
            //reverse the boolean for completer
            //(down <-> up & up <-> down)
            case StressTestTask.TYPE => new StressTestTask.StressTestCompleter(new String(content.slice(1, content.length)).toLong, content(0) != 1)

            case _ => return true
        }
        handler.registerTask(task, packet.taskID, false)
        false
    }

    private def testMap(initPacket: TaskInitPacket, senderId: String, handler: TasksHandler): Unit = {
        val taskType = initPacket.taskType
        if (!completers.contains(taskType))
            throw new TaskException(s"no completer found for task type '$taskType'")
        val supplier = completers(initPacket.taskType)
        supplier(initPacket, handler, senderId)
    }

    override def putCompleter(taskType: String, supplier: (TaskInitPacket, TasksHandler, String) => Unit): Unit =
        completers.put(taskType, supplier)

    private def handleUpload(uploadDesc: TransferDescription, ownerID: String, taskID: Int, handler: TasksHandler): Unit = {
        println(uploadDesc)
        val desc = uploadDesc.reversed(ownerID)
        if (uploadDesc.targetID.equals(server.identifier)) {
            handler.registerTask(DownloadTask(desc), taskID, false)
            return
        }

        val redirectedTransferDesc = new TransferDescriptionBuilder {
            targetID = uploadDesc.targetID
            destination = TempFolder
            source = uploadDesc.source
        }

        /* To perform a upload between two Relay points
        * (let's say RP A want to upload a folder t RP B)
        * the server firsts download the folder from A
        * then upload the downloaded file to B
        * */
        handler.registerTask(DownloadTask(desc), taskID, false)
        handler.registerTask(UploadTask(redirectedTransferDesc), taskID, false)
    }

    private def handleDownload(downloadDesc: TransferDescription, ownerID: String, taskID: Int, handler: TasksHandler): Unit = {
        val desc = downloadDesc.reversed(ownerID)
        if (downloadDesc.targetID.equals(server.identifier)) {
            handler.registerTask(UploadTask(desc), taskID, false)
            return
        }

        val redirectedTransferDesc = new TransferDescriptionBuilder {
            targetID = downloadDesc.targetID
            destination = TempFolder
            source = downloadDesc.source
        }
        /* To perform a download between two Relay points
        * (let's say RP A want to download a folder from RP B)
        * the server firsts download the folder from B
        * then upload the downloaded file to A
        * */
        handler.registerTask(DownloadTask(desc), taskID, false)
        handler.registerTask(UploadTask(redirectedTransferDesc), taskID, false)
    }

}

object ServerTaskCompleterHandler {
    val TempFolder = "/home/override/VPS/FileTransferer/Temp"
}
