package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferableFile
import fr.overridescala.vps.ftp.api.utils.Utils

class FileInfoTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val ownerAddress: InetSocketAddress,
                   private val filePath: String)
        extends Task[TransferableFile](handler, channel.ownerAddress)
                with TaskAchiever {

    override val taskType: TaskType = TaskType.FILE_INFO

    override def preAchieve(): Unit = channel.sendPacket(taskType, filePath, ownerAddress.getHostString.getBytes)


    override def achieve(): Unit = {
        val response = channel.nextPacket()

        if (response.header.equals("ERROR")) {
            error(new String(response.content))
            return
        }
        val size: Long = new String(response.content).toLong
        val transferableFile = TransferableFile.builder()
                .setOwner(ownerAddress)
                .setPath(filePath)
                .setSize(size)
                .build()
        success(transferableFile)
    }

}

object FileInfoTask {

    class Completer(channel: PacketChannel,
                    filePath: String) extends TaskAchiever {

        override val taskType: TaskType = TaskType.FILE_INFO

        override def achieve(): Unit = {
            val path = Path.of(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket(taskType, "ERROR", "The file does not exists.".getBytes())
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket(taskType, "ERROR", "Can't access to the file".getBytes())
                return
            }
            val size = Files.size(path)
            channel.sendPacket(taskType, "OK", s"$size".getBytes())
        }
    }

}