package fr.overridescala.vps.ftp.api.task.tasks

import java.io.File
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.DownloadTask.{ABORT, DOWNLOAD}
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils

/**
 * Downloads a File or folder from a targeted Relay
 *
 * @param desc the description about this transfer
 * @see [[TransferDescription]]
 * */
class DownloadTask(private val handler: TasksHandler,
                   private val desc: TransferDescription)
        extends Task[Unit](handler, desc.targetID) with TaskExecutor {

    private var channel: PacketChannel = _
    private val totalBytes: Float = desc.transferSize
    private var totalBytesWritten = 0

    override def sendTaskInfo(channel: PacketChannel): Unit = {
        channel.sendPacket(DOWNLOAD, Utils.serialize(desc))
    }

    override def execute(channel: PacketChannel): Unit = {
        this.channel = channel
        val response = channel.nextPacket()
        if (response.header.equals(UploadTask.END_OF_TRANSFER)) {
            success()
            return
        }
        val downloadPath = findDownloadPath(response)
        try {
            downloadFile(downloadPath)
        } catch {
            case e: Throwable => {
                e.printStackTrace()
                val typeName = e.getClass.getCanonicalName
                var msg = s"$typeName : ${e.getMessage}"
                if (msg == null)
                    msg = s"got an error of type : $typeName"
                channel.sendPacket(ABORT, msg)
                error(msg)
            }
        }
    }


    private def downloadFile(downloadPath: Path): Unit = {
        println(s"DOWNLOAD START $downloadPath\n")
        if (checkPath(downloadPath))
            return
        val stream = Files.newOutputStream(downloadPath)
        var count = 0

        var packet: DataPacket = channel.nextPacket()
        while (stillForTransfer(packet)) {
            totalBytesWritten += packet.content.length
            stream.write(packet.content)
            count += 1
            channel.sendPacket(s"$count")
            packet = channel.nextPacket()
            val percentage = totalBytesWritten / totalBytes * 100
            print(s"\rsent = $totalBytesWritten, total = $totalBytes, percentage = $percentage, packets sent = $count")
        }
        stream.close()
        println()
        handleLastTransferResponse(packet)
    }

    private def findDownloadPath(packet: DataPacket): Path = {
        Utils.checkPacketHeader(packet, Array(UploadTask.UPLOAD_FILE))
        val root = Utils.formatPath(desc.source.rootPath)
        val uploadedFile = Utils.formatPath(new String(packet.content))
        val destination = Utils.formatPath(new String(desc.destination))
        val rootNameCount = root.toString.count(char => char == File.separatorChar)
        val relativePath = Utils.subPathOfUnknownFile(uploadedFile, rootNameCount)
        val path = Utils.formatPath(destination.toString + relativePath)
        path
    }


    private def handleLastTransferResponse(packet: DataPacket): Unit = {
        val header = packet.header
        Utils.checkPacketHeader(packet, Array(UploadTask.END_OF_TRANSFER, UploadTask.UPLOAD_FILE))
        if (header.equals(UploadTask.END_OF_TRANSFER))
            success()
        else if (header.equals(UploadTask.UPLOAD_FILE)) {
            val downloadPath = findDownloadPath(packet)
            downloadFile(downloadPath)
        }
    }


    /**
     * checks if this packet does not contains ERROR, EOFT or EOT header
     *
     * @return true if the transfer still continue, false instead
     * */
    private def stillForTransfer(packet: DataPacket): Boolean = {
        val header = packet.header
        if (header.equals(ABORT)) {
            val msg = new String(packet.content)
            error(msg)
            throw new TaskException(msg)
        }
        header.matches("[0-9]+")
    }

    /**
     * check the validity of this transfer
     *
     * @return true if the transfer needs to be aborted, false instead
     * */
    private def checkPath(path: Path): Boolean = {
        if (Files.notExists(path)) {
            Files.createDirectories(path)
            Files.delete(path)
            Files.createFile(path)
        }
        if (!Files.isWritable(path) || !Files.isReadable(path)) {
            val errorMsg = "Can't access to the file"
            channel.sendPacket(ABORT, errorMsg.getBytes())
            error(errorMsg)
            return true
        }
        false
    }


}

object DownloadTask {
    val DOWNLOAD: String = "DWN"
    private val ABORT: String = "ERROR"

    def concoct(transferDescription: TransferDescription): TaskConcoctor[Unit] = tasksHandler =>
        new DownloadTask(tasksHandler, transferDescription)


}
