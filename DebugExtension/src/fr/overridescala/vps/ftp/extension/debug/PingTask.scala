package fr.overridescala.vps.ftp.`extension`.debug

import fr.overridescala.vps.ftp.`extension`.debug.PingTask.Type
import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.fundamental.{DataPacket, EmptyPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}

class PingTask(private val targetId: String) extends Task[Long](targetId) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(Type, targetId)

    override def execute(): Unit = {
        channel.sendPacket(EmptyPacket())
        val p1 = testPacket(EmptyPacket())
        val p2 = testPacket(EmptyPacket())
        val p3 = testPacket(EmptyPacket())
        val p4 = testPacket(EmptyPacket())
        val p5 = testPacket(EmptyPacket())
        println("5 packet were tested")
        success((p1 + p2 + p3 + p4 + p5) / 5)
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
    val Type: String = "PING"

    case class Completer() extends TaskExecutor {
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
