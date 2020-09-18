package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.Files

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.{ERROR, FILE_INFO}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.FileDescription
import fr.overridescala.vps.ftp.api.utils.Utils

class FileInfoTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val ownerID: String,
                   private val filePath: String)
        extends Task[FileDescription](handler, ownerID)
                with TaskExecutor {

    override def sendTaskInfo(): Unit = channel.sendPacket(FILE_INFO, Utils.serialize((filePath, ownerID)))

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

    class FileInfoCompleter(channel: PacketChannel,
                            filePath: String) extends TaskExecutor {

        override def execute(): Unit = {
            val path = Utils.formatPath(filePath)
            println("check exists")
            if (Files.notExists(path)) {
                channel.sendPacket(ERROR, s"($path) The file does not exists.".getBytes())
                return
            }
            println("check perms")
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(ERROR, s"($path) Can't access to the file".getBytes())
                return
            }
            println("a")
            val fileInfo = FileDescription.fromLocal(filePath)
            println("b")
            val content = Utils.serialize(fileInfo)
            println("sending packet")
            channel.sendPacket(OK, content)
            println("packet sent")
        }
    }

}