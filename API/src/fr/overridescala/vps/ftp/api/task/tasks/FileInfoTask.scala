package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.{ERROR, FILE_INFO}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferableFile
import fr.overridescala.vps.ftp.api.utils.Utils

class FileInfoTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val ownerAddress: InetSocketAddress,
                   private val filePath: String)
        extends Task[TransferableFile](handler, channel.ownerAddress)
                with TaskExecutor {

    override def sendTaskInfo(): Unit = channel.sendPacket(FILE_INFO, Utils.serialize((filePath, ownerAddress.getHostString)))

    override def execute(): Unit = {
        val response = channel.nextPacket()
        val content = response.content
        if (response.header.equals(ERROR)) {
            error(new String(content))
            return
        }
        success(Utils.deserialize(content))
    }

}

object FileInfoTask {

    val FILE_INFO = "FINFO"
    private val ERROR = "ERROR"
    private val OK = "OK"

    class Completer(channel: PacketChannel,
                    filePath: String) extends TaskExecutor {

        override def execute(): Unit = {
            val path = Path.of(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket(ERROR, s"($path) The file does not exists.".getBytes())
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(ERROR, s"($path) Can't access to the file".getBytes())
                return
            }
            val fileInfo = TransferableFile.fromLocal(filePath)
            val content = Utils.serialize(fileInfo)
            channel.sendPacket(OK, content)
        }
    }

}