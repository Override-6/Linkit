package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.Files

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.{ERROR, FILE_INFO}
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask.StressTestCompleter
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.FileDescription
import fr.overridescala.vps.ftp.api.utils.Utils

class FileInfoTask(private val handler: TasksHandler,
                   private val ownerID: String,
                   private val filePath: String)
        extends Task[FileDescription](handler, ownerID)
                with TaskExecutor {

    override def sendTaskInfo(channel :PacketChannel): Unit = channel.sendPacket(FILE_INFO, Utils.serialize((filePath, ownerID)))

    override def execute(channel :PacketChannel): Unit = {
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

    class FileInfoCompleter(filePath: String) extends TaskExecutor {

        override def execute(channel :PacketChannel): Unit = {
            val path = Utils.formatPath(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket(ERROR, s"($path) The file does not exists.".getBytes())
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(ERROR, s"($path) Can't access to the file".getBytes())
                return
            }
            val fileInfo = FileDescription.fromLocal(filePath)
            val content = Utils.serialize(fileInfo)
            channel.sendPacket(OK, content)
        }
    }

    def concoctCompleter(filePath: String): TaskConcoctor[Unit, FileInfoCompleter] = _ => {
        new FileInfoCompleter(filePath)
    }

    def concoct(ownerID: String, filePath: String): TaskConcoctor[Unit, FileInfoTask] = tasksHandler => {
        new FileInfoTask(tasksHandler, ownerID, filePath)
    }
}