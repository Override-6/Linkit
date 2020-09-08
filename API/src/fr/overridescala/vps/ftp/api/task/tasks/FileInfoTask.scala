package fr.overridescala.vps.ftp.api.task.tasks

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferableFile

class FileInfoTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val ownerAddress: InetSocketAddress,
                   private val filePath: String)
        extends Task[TransferableFile](handler, channel.ownerAddress)
                with TaskAchiever {

    override def preAchieve(): Unit = channel.sendPacket(filePath, ownerAddress.getHostString.getBytes)


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

        override def achieve(): Unit = {
            val path = Path.of(filePath)
            if (Files.notExists(path)) {
                channel.sendPacket("ERROR", "The file does not exists.".getBytes())
                return
            }
            if (!Files.isWritable(path) || !Files.isReadable(path)) {
                channel.sendPacket("ERROR", "Can't access to the file".getBytes())
                return
            }
            val size = Files.size(path)
            channel.sendPacket("OK", s"$size".getBytes())
        }
    }

}