package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.UploadTask.{ABORT, END_OF_TRANSFER, UPLOAD, UPLOAD_FILE}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

class UploadTask(private val channel: PacketChannel,
                 private val handler: TasksHandler,
                 private val desc: TransferDescription)
        extends Task[Unit](handler, channel.ownerAddress) with TaskExecutor {


    override def sendTaskInfo(): Unit = {
        channel.sendPacket(UPLOAD, Utils.serialize(desc))
    }


    override def execute(): Unit = {
        val path = Path.of(desc.source.path)
        val downloadPath = Path.of(desc.destination)
        if (Files.isDirectory(path)) {
            if (!Files.isDirectory(downloadPath)){
                val msg = "download root path have to be a folder path when downloading other folders"
                channel.sendPacket(ABORT, msg)
                error(msg)
                return
            }
            uploadDirectory(path)

        } else uploadFile(path)
        channel.sendPacket(END_OF_TRANSFER)
        success(path)
    }

    private def uploadDirectory(path: Path): Unit = {
        Files.list(path).forEach(children => {
            if (Files.isDirectory(children))
                uploadDirectory(children)
            else uploadFile(children)
        })
    }

    private def uploadFile(path: Path): Unit = {
        if (checkPath(path))
            return
        val stream = Files.newInputStream(path)
        var totalBytesSent: Long = 0
        val totalBytes: Float = desc.transferSize
        var id = -0
        channel.sendPacket(UPLOAD_FILE, path.toString)
        println("UPLOADING " + path)

        while (totalBytesSent < totalBytes) {
            try {
                val bytes = new Array[Byte](Constants.MAX_PACKET_LENGTH - 512)
                totalBytesSent += stream.read(bytes)
                id += 1
                if (makeDataTransfer(bytes, id))
                    return
                val percentage = totalBytesSent / totalBytes * 100
                print(s"\rsent = $totalBytesSent, total = $totalBytes, percentage = $percentage, packets sent = $id")
            } catch {
                case e: Throwable => {
                    var msg = e.getMessage
                    if (msg == null)
                        msg = "an error has  occurred while performing file upload task"
                    channel.sendPacket(ABORT, s"($path) " + msg)
                    return
                }
            }
        }
        println("done !")
        stream.close()
    }

    def checkPath(path: Path): Boolean = {
        if (Files.notExists(path)) {
            val errorMsg = s"($path) could not upload invalid file path : this file does not exists"
            channel.sendPacket(ABORT, errorMsg)
            error(errorMsg)
            return true
        }
        false
    }

    /**
     * makes one data transfer.
     *
     * @return true if the transfer need to be aborted, false instead
     **/
    def makeDataTransfer(bytes: Array[Byte], id: Int): Boolean = {
        channel.sendPacket(s"$id", bytes)
        val packet = channel.nextPacket()
        if (packet.header.equals(ABORT)) {
            error(new String(packet.content))
            return true
        }
        try {
            val packetId = Integer.parseInt(packet.header)
            if (packetId != id) {
                val errorMsg = new String(s"packet id was unexpected (id: $packetId, expected: $id")
                channel.sendPacket(ABORT, errorMsg)
                error(errorMsg)
                return true
            }
        } catch {
            case e: NumberFormatException =>
                channel.sendPacket(ABORT, e.getMessage)
                error(e.getMessage)
        }
        false
    }



}

object UploadTask {
    protected[tasks] val END_OF_TRANSFER: String = "EOT"
    protected[tasks] val UPLOAD_FILE: String = "UPF"
    private val UPLOAD: String = "UP"
    private val ABORT: String = "ERROR"
}
