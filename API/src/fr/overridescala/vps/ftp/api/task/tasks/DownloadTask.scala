package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.DownloadTask.{ABORT, DOWNLOAD, DOWNLOAD_FILE}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils


class DownloadTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val desc: TransferDescription)
        extends Task[Unit](handler, channel.ownerAddress) with TaskExecutor {

    override def sendTaskInfo(): Unit = {
        channel.sendPacket(DOWNLOAD, Utils.serialize(desc))
    }

    override def execute(): Unit = {
        val response = channel.nextPacket()
        val root = Path.of(desc.source.rootPath)
        val uploadedFile = Path.of(new String(response.content))
        println(s"root = ${root}")
        println(s"uploadedFile = ${uploadedFile}")
        val subPath = root.subpath(root.getNameCount - 1, uploadedFile.getNameCount - 1)
        println(s"subPath = ${subPath}")
        val downloadPath = root.relativize(subPath)
        try {
            downloadFile(downloadPath)
        } catch {
            case e: Throwable => {
                e.printStackTrace()
                val typeName = e.getClass.getCanonicalName
                var msg = s"$typeName : ${e.getMessage}"
                if (msg == null)
                    msg = s"got an error of type : $typeName"
                msg = s"Trying to download into file / folder $downloadPath when exception lifted suddenly : " + msg
                channel.sendPacket(ABORT, msg)
                error(msg)
            }
        }
    }


    def downloadFile(downloadPath: Path): Unit = {
        if (checkPath(downloadPath))
            return
        val stream = Files.newOutputStream(downloadPath)
        val totalBytes: Float = desc.transferSize
        var totalBytesWritten = 0
        var id = 0

        var packet: DataPacket = channel.nextPacket()
        while (stillForTransfer(packet)) {
            totalBytesWritten += packet.content.length
            stream.write(packet.content)
            id += 1
            channel.sendPacket(s"$id")
            packet = channel.nextPacket()

            val percentage = totalBytesWritten / totalBytes * 100
            print(s"\rwritten = $totalBytesWritten, total = $totalBytes, percentage = $percentage, packets sent = $id")
        }
        stream.close()

        handleLastTransferResponse(packet)
    }

    def handleLastTransferResponse(packet: DataPacket): Unit = {
        val header = packet.header
        if (header.equals(UploadTask.END_OF_TRANSFER))
            success()
        else if (header.equals(UploadTask.UPLOAD_FILE)) {
            val path = Path.of(new String(packet.content))
            val root = Path.of(desc.source.rootPath)
            val downloadPath = root.relativize(path.subpath(root.getNameCount - 1, path.getNameCount - 1))
            downloadFile(downloadPath)
        } else throw new IllegalArgumentException(s"${UploadTask.END_OF_TRANSFER} or ${UploadTask.UPLOAD_FILE} expected, received : " + packet.toString)
    }


    /**
     * checks if this packet does not contains ERROR, EOFT or EOT header
     *
     * @return true if the transfer still continue, false instead
     **/
    def stillForTransfer(packet: DataPacket): Boolean = {
        val header = packet.header
        if (header.equals(ABORT)) {
            Console.err.println(new String(packet.content))
            error(new String(packet.content))
            return false
        }
        !header.equals(UploadTask.END_OF_TRANSFER)
    }

    /**
     * check the validity of this transfer
     *
     * @return true if the transfer needs to be aborted, false instead
     **/
    def checkPath(path: Path): Boolean = {
        if (Files.notExists(path)) {
            val parent = path.getParent
            if (parent != null && !Files.exists(parent))
                Files.createDirectories(parent)
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
    protected[tasks] val DOWNLOAD_FILE: String = "DWNF"
    protected[tasks] val DOWNLOAD: String = "DWN"
    private val ABORT: String = "ERROR"
}
