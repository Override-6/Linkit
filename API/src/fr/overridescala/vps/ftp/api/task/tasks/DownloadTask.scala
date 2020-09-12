package fr.overridescala.vps.ftp.api.task.tasks

import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, DataPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Utils


class DownloadTask(private val channel: PacketChannel,
                   private val handler: TasksHandler,
                   private val desc: TransferDescription)
        extends Task[Unit](handler, channel.ownerAddress) with TaskExecutor {

    override def sendTaskInfo(): Unit = {
        channel.sendPacket("DWN", Utils.serialize(desc))
    }

    override def execute(): Unit = {
        val response = channel.nextPacket()
        downloadFile(Path.of(new String(response.content)))
    }


    def downloadFile(path: Path): Unit = {
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
            channel.sendPacket(s"$id")
            val percentage = totalBytesWritten / totalBytes * 100
            print(s"written = $totalBytesWritten, total = $totalBytes, percentage = $percentage, packets sent = $id             \r")
        }
        val percentage = totalBytesWritten / totalBytes * 100
        println(s"written = $totalBytesWritten, total = $totalBytes, percentage = $percentage, packets sent = $id")
        stream.close()


        val transferResponse = channel.nextPacket()
        val header = transferResponse.header
        if (header.equals("EOT"))
            success()
        else if (header.equals("UPF"))
            downloadFile(Path.of(new String(transferResponse.content)))
        else throw new IllegalArgumentException("EOF or UPF expected " + transferResponse.toString)
    }


    /**
     * checks if this packet does not contains ERROR Header
     *
     * @return true if the task have to be aborted, false instead
     **/
    def checkPacket(packet: DataPacket): Boolean = {
        if (packet.header.equals("ERROR")) {
            Console.err.println(new String(packet.content))
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
            Files.createDirectories(path.getParent)
            Files.createFile(path)
        }
        if (!Files.isWritable(path) || !Files.isReadable(path)) {
            val errorMsg = "Can't access to the file"
            channel.sendPacket("ERROR", errorMsg.getBytes())
            error(errorMsg)
            return true
        }
        false
    }

}
