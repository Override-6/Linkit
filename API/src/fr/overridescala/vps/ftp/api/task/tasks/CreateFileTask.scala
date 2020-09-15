package fr.overridescala.vps.ftp.api.task.tasks

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask.{CREATE_FILE, ERROR}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}


class CreateFileTask(private val path: String,
                     private val owner: InetSocketAddress,
                     private val channel: PacketChannel,
                     private val tasksHandler: TasksHandler) extends Task[Unit](tasksHandler, owner) {


    override def execute(): Unit = {
        channel.sendPacket(CREATE_FILE, path)
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

    class Completer(private val channel: PacketChannel,
                    private val pathString: String) extends TaskExecutor {


        override def execute(): Unit = {
            val path = Path.of(pathString.replace("\\", "/"))
            val isFile = path.toFile.getName.contains(".")

            createFile(path, isFile)
        }

        def createFile(path: Path, isFile: Boolean): Unit =
            try {
                if (isFile)
                    Files.createFile(path)
                else Files.createDirectories(path)
                channel.sendPacket(OK)
            } catch {
                case e: IOException => {
                    e.printStackTrace()
                    channel.sendPacket(ERROR, e.getMessage)
                }
            }
    }

}

