package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}
import java.util

import fr.overridescala.vps.ftp.api.exceptions.{TaskException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.tasks.UploadTask.{ABORT, END_OF_TRANSFER, TYPE, UPLOAD_FILE}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

/**
 * Uploads a File or Folder to a targeted Relay
 *
 * @param desc the description about this transfer
 * @see [[TransferDescription]]
 * */
class UploadTask(private val desc: TransferDescription)
        extends Task[Unit](desc.targetID) with TaskExecutor {

    private var channel: PacketChannel = _

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, desc.targetID, Utils.serialize(desc))

    override def execute(channel: PacketChannel): Unit = {
        this.channel = channel
        val source = Path.of(desc.source.path)
        val destination = Path.of(desc.destination)

        if (Files.isDirectory(source)) {
            //FIXME add a working folder to folder check.
            /*if (!Files.isDirectory(destination)) {
                val msg = "download root path have to be a folder path when downloading other folders"
                channel.sendPacket(ABORT, msg)
                error(msg)
                return
            }*/
            uploadDirectory(source)

        } else uploadFile(source)
        channel.sendPacket(END_OF_TRANSFER)
        success(source)
    }

    private def uploadDirectory(path: Path): Unit = {
        println(s"UPLOADING DIRECTORY $path")
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
        println("\rUPLOADING " + path)

        while (totalBytesSent < totalBytes) {
            try {
                var bytes = new Array[Byte](Constants.MAX_PACKET_LENGTH - 256)
                val read = stream.read(bytes)
                bytes = util.Arrays.copyOf(bytes, read)
                count += 1
                totalBytesSent += read
                channel.sendPacket("", bytes)

                if (channel.haveMorePackets) {
                    handleUnexpectedPacket(channel.nextPacket())
                }

                val percentage = totalBytesSent / totalBytes * 100
                print(s"\rsent = $totalBytesSent, total = $totalBytes, percentage = $percentage, packets sent = $count")
            } catch {
                case e: Throwable =>
                    var msg = e.getMessage
                    if (msg == null)
                        msg = "an error has occurred while performing file upload task"
                    channel.sendPacket(ABORT, s"($path) " + msg)
                    print("\r")
                    return
            }
        }
        print("\r")
        stream.close()
    }

    private def handleUnexpectedPacket(packet: Packet): Unit = {
        packet match {
            case errorPacket: ErrorPacket =>
                errorPacket.printError()
                error(errorPacket.errorMsg)
            case dataPacket: DataPacket =>
                val header = dataPacket.header
                val errorMsg = s"unexpected packet with header $header was received."
                error(errorMsg)
                throw UnexpectedPacketException(errorMsg)
            case _ => throw UnexpectedPacketException(s"Received unexpected packet of type ${packet.className}")
        }
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
    val TYPE: String = "UP"
    private val ABORT: String = "ERROR"

    def apply(transferDescription: TransferDescription): UploadTask =
        new UploadTask(transferDescription)

}
