package fr.overridescala.vps.ftp.api.packet

import java.net.SocketAddress
import java.nio.channels.ByteChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.task.TasksHandler

class SimplePacketChannel(private val byteChannel: ByteChannel,
                          override val ownerID: String,
                          override val ownerAddress: SocketAddress,
                          private val tasksHandler: TasksHandler)
        extends PacketChannel with PacketChannelManager {

    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](200)

    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val t0 = System.currentTimeMillis()
        val bytes = Protocol.createTaskPacket(tasksHandler.currentSessionID, header, content)
        val t1 = System.currentTimeMillis()
        val bytesTime = t1 - t0
        println(s"time to create bytes : $bytesTime")
        byteChannel.write(bytes)
        val t2 = System.currentTimeMillis()
        val writeTime = t2 - t1
        println("time to send bytes : " + writeTime)
        println()
    }

    override def nextPacket(): DataPacket =
        queue.take()

    override def haveMorePackets: Boolean =
        !queue.isEmpty

    override def addPacket(packet: DataPacket): Boolean = {
        if (packet.sessionID != tasksHandler.currentSessionID)
            queue.clear()
        queue.add(packet)
    }

}