package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.Files

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.{ERROR, TYPE}
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask.StressTestCompleter
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.FileDescription
import fr.overridescala.vps.ftp.api.utils.Utils

/**
 * Retrieves the information about a file / folder such as his size
 *
 * @param ownerID the supposed owner of the file path
 * @param filePath the path of the file / folder to retrieves info
 * */
class FileInfoTask(private val ownerID: String,
                   private val filePath: String)
        extends Task[FileDescription](ownerID)
                with TaskExecutor {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, ownerID, filePath.getBytes)

    override def execute(channel: PacketChannel): Unit = {
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

    val TYPE = "FINFO"
    private val ERROR = "ERROR"
    private val OK = "OK"

    class FileInfoCompleter(filePath: String) extends TaskExecutor {

        override def execute(channel: PacketChannel): Unit = {
            val path = Utils.formatPath(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket(ERROR, s"($path) The file does not exists.")
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(ERROR, s"($path) Can't access to the file")
                return
            }
            val fileInfo = FileDescription.fromLocal(filePath)
            val content = Utils.serialize(fileInfo)
            channel.sendPacket(OK, content)
        }
    }

    def apply(ownerID: String, filePath: String): FileInfoTask =
        new FileInfoTask(ownerID, filePath)
}