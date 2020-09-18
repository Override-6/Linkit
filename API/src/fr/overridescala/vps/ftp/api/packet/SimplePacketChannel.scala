package fr.overridescala.vps.ftp.api.packet

import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.task.TasksHandler

class SimplePacketChannel(private val socketChannel: SocketChannel,
                          override val ownerID: String,
                          private val tasksHandler: TasksHandler)
        extends PacketChannel with PacketChannelManager {

    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](200)

    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val bytes = Protocol.createTaskPacket(tasksHandler.currentSessionID, header, content)
        socketChannel.write(bytes)
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

    override val ownerAddress: SocketAddress = socketChannel.getRemoteAddress
}