package fr.overridescala.vps.ftp.api.task.tasks

import java.text.{DecimalFormat, NumberFormat}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask.{TYPE, download, upload}
import fr.overridescala.vps.ftp.api.task._
import fr.overridescala.vps.ftp.api.utils.Constants

/**
 * This is a Test task, will not be documented.
 *
 * wait
 * */
class StressTestTask(private val totalDataLength: Long,
                     private val isDownload: Boolean,
                     private val async: Boolean) extends Task[Unit](Constants.SERVER_ID) {

    override val initInfo: TaskInitInfo = {
        val downloadBit: Byte = if (isDownload) 1 else 0
        val asyncBit: Byte = if (async) 1 else 0
        TaskInitInfo.of(TYPE, Constants.SERVER_ID, Array(downloadBit) ++ Array(asyncBit) ++ s"$totalDataLength".getBytes())
    }

    override def execute(channel: PacketChannel): Unit = {
        if (isDownload)
            download(channel, totalDataLength, async)
        else upload(channel, totalDataLength, async)
        success()
    }


}

object StressTestTask {

    private val CONTINUE = "PCKT"
    private val END = "END"
    val TYPE = "STRSS"


    class StressTestCompleter(private val totalDataLength: Long, isDownload: Boolean, waitResponsePacket: Boolean) extends TaskExecutor {
        override def execute(channel: PacketChannel): Unit = {
            if (isDownload)
                download(channel, totalDataLength, waitResponsePacket)
            else upload(channel, totalDataLength, waitResponsePacket)
        }
    }

    private def upload(channel: PacketChannel, totalDataLength: Long, async: Boolean): Unit = {
        println("UPLOAD")
        var totalSent: Float = 0
        val totalDataLengthFormatted = format(totalDataLength)
        val capacity = Constants.MAX_PACKET_LENGTH - 128
        var bytes = new Array[Byte](capacity)
        var maxBPS = 0F
        while (totalSent < totalDataLength) {
            if (totalDataLength - totalSent < capacity)
                bytes = new Array[Byte]((totalDataLength - totalSent).asInstanceOf[Int])

            val t0 = System.currentTimeMillis()
            channel.sendPacket(CONTINUE, bytes)
            if (!async)
                channel.nextPacket()
            val t1 = System.currentTimeMillis()
            val time: Float = t1 - t0

            totalSent += capacity

            val percentage = totalSent / totalDataLength * 100
            var bps = capacity / (time / 1000)
            if (bps == Float.PositiveInfinity)
                bps = 0
            maxBPS = Math.max(bps, maxBPS)
            print(s"\rjust sent ${capacity} in $time ms ${format(bps)} bytes/s (${format(totalSent)} / $totalDataLengthFormatted $percentage%) (max b/s = ($maxBPS)")
        }
        channel.sendPacket(END)
        println()
    }

    private def download(channel: PacketChannel, totalDataLength: Long, async: Boolean): Unit = {
        println("DOWNLOAD")
        var packet = channel.nextPacket()
        var totalReceived: Float = 0
        val totalDataLengthFormatted = format(totalDataLength)
        var maxBPS = 0F
        while (packet.header.equals(CONTINUE)) {
            val t0 = System.currentTimeMillis()
            if (!async)
                channel.sendPacket(CONTINUE)
            packet = channel.nextPacket()
            val dataLength = packet.content.length
            val t1 = System.currentTimeMillis()
            val time: Float = t1 - t0

            totalReceived += dataLength

            val percentage = totalReceived / totalDataLength * 100
            var bps = dataLength / (time / 1000)
            if (bps == Float.PositiveInfinity)
                bps = 0
            maxBPS = Math.max(bps, maxBPS)
            print(s"\rjust received ${dataLength} in $time ms ${format(bps)}  bytes/s (${format(dataLength)} / $totalDataLengthFormatted $percentage%) (max b/s = ($maxBPS)")
        }
    }

    private def format(value: Float): String =
        new DecimalFormat("### ### ### ### ### ###").format(value)

    def apply(totalDataLength: Int, isDownload: Boolean, async: Boolean): StressTestTask =
        new StressTestTask(totalDataLength, isDownload, async)


}
