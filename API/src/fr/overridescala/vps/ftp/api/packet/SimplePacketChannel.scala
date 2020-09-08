package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.task.{TaskType, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Protocol

class SimplePacketChannel(private val socketChannel: SocketChannel,
                         private val tasksHandler: TasksHandler)
        extends PacketChannel with PacketChannelManager {

    private val queue: BlockingQueue[TaskPacket] = new ArrayBlockingQueue[TaskPacket](200)

    override val ownerAddress: InetSocketAddress = socketChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]

    override def sendPacket(taskType: TaskType, header: String, content: Array[Byte] = Array()): Unit = {
        val bytes = Protocol.createTaskPacket(tasksHandler.currentSessionID, taskType, header, content)
        socketChannel.write(bytes)
    }

    override def nextPacket(): TaskPacket = {
        queue.take()
    }

    override def addPacket(packet: TaskPacket): Boolean = {
        queue.add(packet)
    }

}