package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask.{CONTINUE, END, TYPE, download, upload}
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

/**
 * This is a Test task, will not be documented.
 *
 * wait
 * */
class StressTestTask(private val handler: TasksHandler,
                     private val totalDataLength: Long,
                     private val isDownload: Boolean) extends Task[Unit](handler, Constants.SERVER_ID) {

    override val initInfo: TaskInitInfo = {
        val bit: Byte = if (isDownload) 1 else 0
        TaskInitInfo.of(TYPE, Constants.SERVER_ID, Array(bit) ++ s"$totalDataLength".getBytes())
    }

    override def execute(channel: PacketChannel): Unit = {
        if (isDownload)
            download(channel, totalDataLength)
        else upload(channel, totalDataLength)
        success()
    }


}

object StressTestTask {

    private val CONTINUE = "PCKT"
    private val END = "END"
    val TYPE = "STRSS"


    class StressTestCompleter(private val totalDataLength: Long, isDownload: Boolean) extends TaskExecutor {
        override def execute(channel: PacketChannel): Unit = {
            if (isDownload)
                download(channel, totalDataLength)
            else upload(channel, totalDataLength)
        }
    }

    private def upload(channel: PacketChannel, totalDataLength: Long): Unit = {
        println("DOING UPLOAD")
        var totalSent: Float = 0
        val capacity = Constants.MAX_PACKET_LENGTH - 512
        var bytes = new Array[Byte](capacity)
        while (totalSent < totalDataLength) {
            if (totalDataLength - totalSent < capacity)
                bytes = new Array[Byte]((totalDataLength - totalSent).asInstanceOf[Int])

            val t0 = System.currentTimeMillis()
            channel.sendPacket(CONTINUE, bytes)
            channel.nextPacket()
            val t1 = System.currentTimeMillis()
            val time: Float = t1 - t0

            totalSent += capacity

            val percentage = totalSent / totalDataLength * 100
            print(s"\rjust sent ${capacity} in $time ms ${capacity / (time / 1000)} bytes/s ($totalSent / $totalDataLength $percentage%)")
        }
        channel.sendPacket(END)
        println()
    }

    private def download(channel: PacketChannel, totalDataLength: Long): Unit = {
        println("DOING DOWNLOAD")
        var packet = channel.nextPacket()
        var totalReceived: Float = 0
        while (packet.header.equals(CONTINUE)) {
            val t0 = System.currentTimeMillis()
            channel.sendPacket(CONTINUE)
            packet = channel.nextPacket()
            val dataLength = packet.content.length
            val t1 = System.currentTimeMillis()
            val time: Float = t1 - t0

            totalReceived += dataLength

            val percentage = totalReceived / totalDataLength * 100
            val bps = dataLength / (time / 1000)
            print(s"\rjust received ${dataLength} in $time ms $bps  bytes/s ($totalReceived / $totalDataLength $percentage%)")
        }
    }

    def concoct(totalDataLength: Int, isDownload: Boolean): TaskConcoctor[Unit] = tasksHandler => {
        new StressTestTask(tasksHandler, totalDataLength, isDownload)
    }

}
