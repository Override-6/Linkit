package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.Files

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.TYPE
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
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
        extends Task[FileDescription](ownerID) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, ownerID, filePath.getBytes)

    override def execute(): Unit = {
        val response = channel.nextPacket()
        response match {
            case errorPacket: ErrorPacket =>
                errorPacket.printError()
                error(errorPacket.errorMsg)
            case data: DataPacket => success(Utils.deserialize(data.content))
        }
    }

}

object FileInfoTask {

    val TYPE = "FINFO"
    private val OK = "OK"

    class FileInfoCompleter(filePath: String) extends TaskExecutor {

        override def execute(): Unit = {
            val path = Utils.formatPath(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket(ErrorPacket("File not found", s"file not found for path '$path"))
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(ErrorPacket("NoSuchPermissions", s"can't access to file/folder '$path'", "Unable to write or read"))
                return
            }
            val fileInfo = FileDescription.fromLocal(filePath)
            val content = Utils.serialize(fileInfo)
            channel.sendPacket(DataPacket(OK, content))
        }
    }

    def apply(ownerID: String, filePath: String): FileInfoTask =
        new FileInfoTask(ownerID, filePath)
}