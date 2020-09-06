package fr.overridescala.vps.ftp.api.packet

import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.utils.Protocol

class SimplePacketChannel(private val socketChannel: SocketChannel)
        extends PacketChannel {

    private val queue: BlockingQueue[TaskPacket] = new ArrayBlockingQueue[TaskPacket](200)

    override def sendPacket(packet: TaskPacket): Unit = {
        socketChannel.write(Protocol.createTaskPacket(packet.taskType, packet.header, packet.content))
    }

    override def nextPacket(): TaskPacket = {
        queue.take()
    }

    def addPacket(packet: TaskPacket): Unit = {
        queue.add(packet)
    }



}
