package fr.overridescala.vps.ftp.api.packet

import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException

class SimplePacketChannel(private val socket: SocketChannel,
                          override val ownerID: String,
                          override val taskID: Int)
        extends PacketChannel with PacketChannelManager {

    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](200)

    override val ownerAddress: SocketAddress = socket.getRemoteAddress

    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val bytes = Protocol.createTaskPacket(taskID, header, content)
        socket.write(bytes)
    }

    override def nextPacket(): DataPacket = try {
        queue.take()
    }

    override def haveMorePackets: Boolean =
        !queue.isEmpty

    override def addPacket(packet: DataPacket): Unit = {
        if (packet.taskID != taskID)
            throw UnexpectedPacketException("packet sessions differs ! ")
        queue.add(packet)
    }
}