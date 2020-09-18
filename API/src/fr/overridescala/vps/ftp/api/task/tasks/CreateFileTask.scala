package fr.overridescala.vps.ftp.api.task.tasks

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask.{CREATE_FILE, ERROR}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils


class CreateFileTask(private val path: String,
                     private val ownerID: String,
                     private val channel: PacketChannel,
                     private val tasksHandler: TasksHandler) extends Task[Unit](tasksHandler, ownerID) {


    override def sendTaskInfo(): Unit =
        channel.sendPacket(CREATE_FILE, path)

    override def execute(): Unit = {
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

    class CreateFileCompleter(private val channel: PacketChannel,
                              private val pathString: String) extends TaskExecutor {


        override def execute(): Unit = {
            println("a")
            val path = Utils.formatPath(pathString)
            println("b")
            val isFile = path.toFile.getName.contains(".")
            println("c")
            createFile(path, isFile)
            println("task end")
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

}

