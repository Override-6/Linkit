package fr.overridescala.vps.ftp.`extension`.fundamental

import fr.overridescala.vps.ftp.`extension`.fundamental.PingTask.TYPE
import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, EmptyPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}

class PingTask(private val targetId: String) extends Task[Long](targetId) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetId)

    override def execute(): Unit = {
        val p1 = testPacket(EmptyPacket())
        val p2 = testPacket(DataPacket(""))
        val p3 = testPacket(ErrorPacket("", ""))
        success((p1 + p2 + p3) / 4)
    }

    def testPacket(packet: Packet): Long = {
        val t0 = System.currentTimeMillis()
        channel.sendPacket(packet)
        channel.nextPacket()
        val t1 = System.currentTimeMillis()
        val time = t1 - t0
        println(packet.getClass.getSimpleName + s" sent and received in $time ms")
        time
    }

}

object PingTask {
    val TYPE: String = "PING"

    class Completer extends TaskExecutor {
        override def execute(): Unit = {
            val pong = EmptyPacket()
            for (_ <- 1 to 3) {
                channel.nextPacket()
                channel.sendPacket(pong)
            }
        }

    }

    def apply(targetID: String): PingTask =
        new PingTask(targetID)


}
