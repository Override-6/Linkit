package fr.overridescala.vps.ftp.api.task.tasks

import java.io.File
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.time.Instant

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.tasks.DownloadTask.{ABORT, TYPE}
import fr.overridescala.vps.ftp.api.task._
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils

/**
 * Downloads a File or folder from a targeted Relay
 *
 * @param desc the description about this transfer
 * @see [[TransferDescription]]
 * */
class DownloadTask private(private val desc: TransferDescription)
        extends Task[Unit](desc.targetID) {

    private val totalBytes: Float = desc.transferSize
    private var totalBytesWritten = 0

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, desc.targetID, Utils.serialize(desc))

    override def execute(): Unit = {

        val response = nextPacket():DataPacket
        //empty upload
        if (response.header == UploadTask.END_OF_TRANSFER) {
            success()
            return
        }
        val downloadPath = findDownloadPath(response)
        try {
            downloadFile(downloadPath)
        } catch {
            case e: Throwable =>
                e.printStackTrace()
                val typeName = e.getClass.getCanonicalName
                var msg = s"$typeName : ${e.getMessage}"
                if (msg == null)
                    msg = s"got an error of type : $typeName"
                channel.sendPacket(ErrorPacket(ABORT, msg))
                error(msg)
        }
        println("download end")
    }


    private def downloadFile(downloadPath: Path): Unit = {
        println(s"DOWNLOAD START $downloadPath")
        if (checkPath(downloadPath))
            return
        val stream = Files.newOutputStream(downloadPath)
        var count = 0
        var packet = nextPacket(): DataPacket

        def downloading: Boolean = packet.header != UploadTask.UPLOAD_FILE && packet.header != UploadTask.END_OF_TRANSFER

        while (downloading) {
            totalBytesWritten += packet.content.length
            stream.write(packet.content)
            count += 1
            packet = nextPacket(): DataPacket
            val percentage = totalBytesWritten / totalBytes * 100
            print(s"\rreceived = $totalBytesWritten, total = $totalBytes, percentage = $percentage, packets exchange = $count")
        }
        //TODO remove this line
        Files.setLastModifiedTime(downloadPath, FileTime.from(Instant.now))
        print("\r")
        stream.close()
        handleLastTransferResponse(packet)
    }


    private def findDownloadPath(packet: DataPacket): Path = {
        Utils.checkPacketHeader(packet, Array("UPF"))
        val root = Utils.formatPath(desc.source.rootPath)
        val rootNameCount = root.toString.count(char => char == File.separatorChar)

        val uploadedFile = Utils.formatPath(new String(packet.content))
        val destination = Utils.formatPath(new String(desc.destination))

        val relativePath = Utils.subPathOfUnknownFile(uploadedFile, rootNameCount)
        Utils.formatPath(destination.toString + relativePath)
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
            channel.sendPacket(ErrorPacket(ABORT, errorMsg))
            error(errorMsg)
            return true
        }
        false
    }

    private def nextPacket[P <: Packet](): P = {
        val packet = channel.nextPacket()
        packet match {
            case error: ErrorPacket =>
                throw new TaskException(error.errorMsg)
            case _ => packet.asInstanceOf[P]
        }
    }


}

object DownloadTask {
    val TYPE: String = "DWN"
    private val ABORT: String = "ERROR"

    def apply(transferDescription: TransferDescription): DownloadTask =
        new DownloadTask(transferDescription)


}
