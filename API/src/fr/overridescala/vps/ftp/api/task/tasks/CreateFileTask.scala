package fr.overridescala.vps.ftp.api.task.tasks

import java.io.IOException
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask.{CREATE_FILE, ERROR}
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils

/**
 * Ask to create a File / folder to the targeted ownerID
 * */
class CreateFileTask(private val tasksHandler: TasksHandler,
                     private val ownerID: String,
                     private val path: String,
                     private val isDirectory: Boolean) extends Task[Unit](tasksHandler, ownerID) {


    override def sendTaskInfo(channel: PacketChannel): Unit = {
        val bit: Byte = if (isDirectory) 1 else 0
        channel.sendPacket(CREATE_FILE, Array(bit) ++ path.getBytes)
    }

    override def execute(channel: PacketChannel): Unit = {
        val packet = channel.nextPacket()
        val header = packet.header
        val content = packet.content
        if (header.equals(ERROR)) {
            error(new String(content))
            return
        }
        success()
    }
}

object CreateFileTask {
    val CREATE_FILE = "CRTF"
    private val ERROR = "ERROR"
    private val OK = "OK"

    /**
     * Creates a File / Folder to the desired path
     * @param pathString the file / folder path to be created
     * */
    class CreateFileCompleter(private val pathString: String,
                              private val isDirectory: Boolean) extends TaskExecutor {

        private var channel: PacketChannel = _

        override def execute(channel: PacketChannel): Unit = {
            this.channel = channel
            val path = Utils.formatPath(pathString)
            createFile(path)
        }

        def createFile(path: Path): Unit =
            try {
                if (Files.exists(path)) {
                    channel.sendPacket(ERROR, "$path already exists !")
                    return
                }
                if (!isDirectory)
                    Files.createFile(path)
                else Files.createDirectories(path)
                channel.sendPacket(OK)
            } catch {
                case e: IOException =>
                    e.printStackTrace()
                    channel.sendPacket(ERROR, e.getMessage)
            }
    }

    def concoct(ownerID: String, filePath: String, isDirectory: Boolean): TaskConcoctor[Unit] = tasksHandler =>
        new CreateFileTask(tasksHandler, ownerID, filePath, isDirectory)


}