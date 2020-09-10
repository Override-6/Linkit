package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.task.TasksHandler

class SimplePacketChannel(private val socketChannel: SocketChannel,
                         private val tasksHandler: TasksHandler)
        extends PacketChannel with PacketChannelManager {

    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](200)

    override val ownerAddress: InetSocketAddress = socketChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]

    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val bytes = Protocol.createTaskPacket(tasksHandler.currentSessionID, header, content)
        //only debug
        Protocol.toPacket(bytes.array())
        socketChannel.write(bytes)
    }

    override def nextPacket(): DataPacket = {
        queue.take()
    }

    override def addPacket(packet: DataPacket): Boolean = {
        queue.add(packet)
    }

}