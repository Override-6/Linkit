package fr.overridescala.vps.ftp.api.task.tasks

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask.{CREATE_FILE, ERROR}
import fr.overridescala.vps.ftp.api.task.tasks.FileInfoTask.FileInfoCompleter
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils


class CreateFileTask(private val tasksHandler: TasksHandler,
                     private val ownerID: String,
                     private val path: String) extends Task[Unit](tasksHandler, ownerID) {


    override def sendTaskInfo(channel :PacketChannel): Unit =
        channel.sendPacket(CREATE_FILE, path)

    override def execute(channel :PacketChannel): Unit = {
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

    class CreateFileCompleter(private val pathString: String) extends TaskExecutor {

        private var channel: PacketChannel = _

        override def execute(channel :PacketChannel): Unit = {
            this.channel = channel
            val path = Utils.formatPath(pathString)
            val isFile = path.toFile.getName.contains(".")
            createFile(path, isFile)
        }

        def createFile(path: Path, isFile: Boolean): Unit =
            try {
                println("creating file or folder")
                if (isFile)
                    Files.createFile(path)
                else Files.createDirectories(path)
                println("sending packet")
                channel.sendPacket(OK)
                println("packet sent")
            } catch {
                case e: IOException => {
                    e.printStackTrace()
                    channel.sendPacket(ERROR, e.getMessage)
                }
            }
    }


    def concoctCompleter(filePath: String): TaskConcoctor[Unit, CreateFileCompleter] = _ => {
        new CreateFileCompleter(filePath)
    }

    def concoct(ownerID: String, filePath: String): TaskConcoctor[Unit, CreateFileTask] = tasksHandler => {
        new CreateFileTask(tasksHandler, ownerID, filePath)
    }



}

