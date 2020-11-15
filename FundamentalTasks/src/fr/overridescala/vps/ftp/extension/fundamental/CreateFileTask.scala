package fr.overridescala.vps.ftp.`extension`.fundamental

import java.io.IOException
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.fundamental.{DataPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import fr.overridescala.vps.ftp.api.utils.Utils
import CreateFileTask.TYPE

/**
 * Ask to create a File / folder to the targeted ownerID
 * */
class CreateFileTask private(private val ownerID: String,
                             private val path: String,
                             private val isDirectory: Boolean) extends Task[Unit](ownerID) {


    override val initInfo: TaskInitInfo = {
        val bit: Byte = if (isDirectory) 1 else 0
        TaskInitInfo.of(TYPE, ownerID, Array(bit) ++ path.getBytes)
    }

    override def execute(): Unit = {
        channel.nextPacket() match {
            case errorPacket: ErrorPacket =>
                errorPacket.printError()
                fail(errorPacket.errorMsg)
            case _: DataPacket => success()
        }
        success()
    }
}

object CreateFileTask {
    val TYPE = "CRTF"
    private val ERROR = "ERROR"
    private val OK = "OK"

    /**
     * Creates a File / Folder to the desired path
     * @param pathString the file / folder path to be created
     * */
    class Completer(private val pathString: String,
                    private val isDirectory: Boolean) extends TaskExecutor {

        override def execute(): Unit = {
            this.channel = channel
            val path = Utils.formatPath(pathString)
            createFile(path)
        }

        def createFile(path: Path): Unit =
            try {
                if (Files.exists(path)) {

                    channel.sendPacket(ErrorPacket("File already exists", s"the file set to peth '$path' already exists"))
                    return
                }
                if (!isDirectory)
                    Files.createFile(path)
                else Files.createDirectories(path)
                channel.sendPacket(DataPacket(OK))
            } catch {
                case e: IOException =>
                    e.printStackTrace()
                    channel.sendPacket(DataPacket(ERROR, e.getMessage))
            }
    }

    def apply(ownerID: String, filePath: String, isDirectory: Boolean): CreateFileTask =
        new CreateFileTask(ownerID, filePath, isDirectory)


}