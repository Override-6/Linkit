package fr.overridescala.vps.ftp.api.task.tasks

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskConcoctor, TaskExecutor, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Constants

/**
 * This is a Test task, will not be documented.
 *
 * wait
 * */
class StressTestTask(private val handler: TasksHandler,
                     private val totalDataLength: Long) extends Task[Unit](handler, Constants.SERVER_ID) {

    override def sendTaskInfo(channel :PacketChannel): Unit = {
        channel.sendPacket("STRSS", s"$totalDataLength")
    }

    override def execute(channel :PacketChannel): Unit = {
        var totalSent: Float = 0
        val capacity = Constants.MAX_PACKET_LENGTH - 512
        var bytes = new Array[Byte](capacity)
        while (totalSent < totalDataLength) {
            if (totalDataLength - totalSent < capacity)
                bytes = new Array[Byte]((totalDataLength - totalSent).asInstanceOf[Int])

            val t0 = System.currentTimeMillis()
            channel.sendPacket("PCKT", bytes)
            channel.nextPacket()
            val t1 = System.currentTimeMillis()
            val time: Float = t1 - t0

            totalSent += capacity

            val percentage = totalSent / totalDataLength * 100
            print(s"\rjust sent ${capacity} in $time ms ${capacity / (time / 1000)} bytes/s ($totalSent / $totalDataLength $percentage%)")
        }
        channel.sendPacket("END")
    }


}

object StressTestTask {

    class StressTestCompleter(private val totalDataLength: Long) extends TaskExecutor {
        override def execute(channel :PacketChannel): Unit = {
            var packet = channel.nextPacket()
            var totalReceived: Float = 0
            while (packet.header.equals("PCKT")) {
                val t0 = System.currentTimeMillis()
                channel.sendPacket("OK")
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
    }

    def concoct(totalDataLength: Int): TaskConcoctor[Unit] = tasksHandler => {
        new StressTestTask(tasksHandler, totalDataLength)
    }

}
