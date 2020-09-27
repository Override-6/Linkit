package fr.overridescala.vps.ftp.api.packet

import java.nio.channels.SocketChannel
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException

/**
 * this class is the implementation of [[PacketChannel]] and [[PacketChannelManager]]
 *
 * @param socket the socket where packets will be sent
 * @param taskID the taskID attributed to this PacketChannel
 *
 * @see [[PacketChannel]]
 * @see [[PacketChannelManager]]
 * */
class SimplePacketChannel(private val socket: SocketChannel,
                          override val taskID: Int)
        extends PacketChannel with PacketChannelManager {

    /**
     * this blocking queue stores the received packets until they are requested
     * */
    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](200)

    /**
     * Builds a [[DataPacket]] from a header string and a content byte array,
     * then send it to the targeted Relay that complete the task
     *
     * @param header the packet header
     * @param content the packet content
     * */
    override def sendPacket(header: String, content: Array[Byte] = Array()): Unit = {
        val bytes = Protocol.createDataPacket(taskID, header, content)
        socket.write(bytes)
    }

    /**
     * Waits until a data packet is received and concerned about this task.
     *
     * @return the received packet
     * @see [[DataPacket]]
     * */
    override def nextPacket(): DataPacket =
        queue.take()

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    override def haveMorePackets: Boolean =
        !queue.isEmpty

    /**
     * add a packet into the PacketChannel. the PacketChannel will stop waiting in [[PacketChannel#nextPacket]] if it where waiting for a packet
     *
     * @param packet the packet to add
     * @throws UnexpectedPacketException if the packet id not equals the channel task ID
     * */
    override def addPacket(packet: DataPacket): Unit = {
        if (packet.taskID != taskID)
            throw UnexpectedPacketException("packet sessions differs ! ")
        queue.add(packet)
    }
}