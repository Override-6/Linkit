package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAchiever, TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils


class DownloadTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val desc: TransferDescription)
        extends Task[Unit]() with TaskAchiever {

    override val taskType: TaskType = TaskType.DOWNLOAD

    override def enqueue(): Unit = handler.register(this, channel.getOwnerAddress, true)

    override def preAchieve(): Unit = {
        val packet = new TaskPacket(taskType, "TD", Utils.serialize(desc))
        channel.sendPacket(packet)
    }

    override def achieve(): Unit = {
        val path = Path.of(desc.destination)
        if (checkPath(path))
            return
        val stream = Files.newOutputStream(path)
        val totalBytes: Float = desc.transferSize
        var totalBytesWritten = 0
        var id = 0
        while (totalBytesWritten < totalBytes) {
            val data = channel.nextPacket()
            if (checkPacket(data))
                return
            totalBytesWritten += data.content.length
            stream.write(data.content)
            id += 1
            channel.sendPacket(new TaskPacket(taskType, s"$id"))
            val percentage = totalBytesWritten / totalBytes * 100
            print(s"written = $totalBytesWritten, total = $totalBytes, percentage = $percentage\r")
        }
        val percentage = totalBytesWritten / totalBytes * 100
        println(s"written = $totalBytesWritten, total = $totalBytes, percentage = $percentage\r")
        success(path)
    }

    /**
     * checks if this packet does not contains ERROR Header
     *
     * @return true if the task have to be aborted, false instead
     **/
    def checkPacket(packet: TaskPacket): Boolean = {
        if (packet.header.equals("ERROR")) {
            error(new String(packet.content))
            return true
        }
        false
    }

    /**
     * check the validity of this transfer
     *
     * @return true if the transfer needs to be aborted, false instead
     **/
    def checkPath(path: Path): Boolean = {
        if (Files.notExists(path)) {
            Files.createFile(path)
        }
        if (!Files.isWritable(path) || !Files.isReadable(path)) {
            val errorMsg = "Can't access to the file"
            channel.sendPacket(new TaskPacket(taskType, "ERROR", errorMsg.getBytes()))
            error(errorMsg)
            return true
        }
        false
    }

}
