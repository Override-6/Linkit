package fr.`override`.linkit.extension.debug

import fr.`override`.linkit.extension.debug.PingTask.Type

class PingTask(private val targetId: String) extends Task[Long](targetId) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(Type, targetId)


    /**
     * adding some scala doc in order to commit something
     * */
    override def execute(): Unit = {
        println(s"(channel id ${channel.identifier})")
        channel.send(EmptyPacket)
        val p1 = testPacket(EmptyPacket)
        val p2 = testPacket(EmptyPacket)
        val p3 = testPacket(EmptyPacket)
        val p4 = testPacket(EmptyPacket)
        val p5 = testPacket(EmptyPacket)
        println("5 packet were tested")
        success((p1 + p2 + p3 + p4 + p5) / 5)
    }

    def testPacket(packet: Packet): Long = {
        val t0 = System.currentTimeMillis()

        channel.send(packet)
        channel.nextPacket

        val t1 = System.currentTimeMillis()
        val time = t1 - t0
        println(packet.getClass.getSimpleName + s" sent and received in $time ms")
        time
    }

}

object PingTask {
    val Type: String = "PING"

    def apply(targetID: String): PingTask =
        new PingTask(targetID)

    case class Completer() extends TaskExecutor {
        override def execute(): Unit = {
            channel.nextPacket
            for (_ <- 1 to 5) {
                channel.nextPacket
                channel.send(EmptyPacket)
            }
        }

    }


}
