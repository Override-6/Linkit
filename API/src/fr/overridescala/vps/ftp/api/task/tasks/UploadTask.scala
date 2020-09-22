package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}
import java.util

import fr.overridescala.vps.ftp.api.exceptions.{TransferException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.UploadTask.{ABORT, END_OF_TRANSFER, UPLOAD, UPLOAD_FILE}
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

class UploadTask(private val handler: TasksHandler,
                 private val desc: TransferDescription)
        extends Task[Unit](handler, desc.targetID) with TaskExecutor {

    private var channel: PacketChannel = _

    override def sendTaskInfo(channel :PacketChannel): Unit = {
        channel.sendPacket(UPLOAD, Utils.serialize(desc))
    }


    override def execute(channel :PacketChannel): Unit = {
        this.channel = channel;
        val path = Path.of(desc.source.path)
        val destination = Path.of(desc.destination)
        println(s"path = ${path}")
        println(s"destination = ${destination}")

        if (Files.isDirectory(path)) {
            if (Files.notExists(destination))
                Files.createDirectories(destination)
            else if (!Files.isDirectory(destination)) {
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
        println(s"UPLOADING DIRECTORY ${path}")
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
        val totalBytes: Float = Files.size(path)
        var count = 0
        channel.sendPacket(UPLOAD_FILE, path.toString)
        println("UPLOADING " + path)

        while (totalBytesSent < totalBytes) {
            try {
                var bytes = new Array[Byte](Constants.MAX_PACKET_LENGTH - 256)
                val read = stream.read(bytes)
                bytes = util.Arrays.copyOf(bytes, read)
                count += 1
                totalBytesSent += read
                makeDataTransfer(bytes, count)

                if (channel.haveMorePackets) {
                    handleUnexpectedPacket(channel.nextPacket())
                }

                val percentage = totalBytesSent / totalBytes * 100
                print(s"\rsent = $totalBytesSent, total = $totalBytes, percentage = $percentage, packets sent = $count")
            } catch {
                case e: Throwable => {
                    var msg = e.getMessage
                    if (msg == null)
                        msg = "an error has occurred while performing file upload task"
                    channel.sendPacket(ABORT, s"($path) " + msg)
                    return
                }
            }
        }
        println()
        stream.close()
    }

    /**
     * makes one data transfer.
     *
     * @return true if the transfer need to be aborted, false instead
     * */
    private def makeDataTransfer(bytes: Array[Byte], id: Int): Unit = {
        channel.sendPacket(s"$id", bytes)
        val packet = channel.nextPacket()
        if (packet.header.equals(ABORT)) {
            val errorMsg = new String(packet.content)
            error(errorMsg)
            throw new TransferException(errorMsg)
        }
        try {
            val packetId = Integer.parseInt(packet.header)
            if (packetId != id) {
                val errorMsg = new String(s"packet id was unexpected (id: $packetId, expected: $id)")
                channel.sendPacket(ABORT, errorMsg)
                error(errorMsg)
                throw new TransferException(errorMsg)
            }
        } catch {
            case e: NumberFormatException =>
                channel.sendPacket(ABORT, e.getMessage)
                error(e.getMessage)
        }
    }

    private def handleUnexpectedPacket(packet: DataPacket): Unit = {
        val header = packet.header
        val content = packet.content
        if (header.equals(ABORT)) {
            error(new String(content))
            return
        }
        throw UnexpectedPacketException(s"unexpected packet with header $header was received.")
    }

    private def checkPath(path: Path): Boolean = {
        if (Files.notExists(path)) {
            val errorMsg = s"($path) could not upload invalid file path : this file does not exists"
            channel.sendPacket(ABORT, errorMsg)
            error(errorMsg)
            return true
        }
        false
    }


}

object UploadTask {
    protected[tasks] val END_OF_TRANSFER: String = "EOT"
    protected[tasks] val UPLOAD_FILE: String = "UPF"
    val UPLOAD: String = "UP"
    private val ABORT: String = "ERROR"

    def concoct(transferDescription: TransferDescription): TaskConcoctor[Unit, UploadTask] = tasksHandler => {
        new UploadTask(tasksHandler, transferDescription)
    }

}
