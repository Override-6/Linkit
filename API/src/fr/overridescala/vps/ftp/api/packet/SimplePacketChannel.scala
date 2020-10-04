package fr.overridescala.vps.ftp.api.packet

import java.io.{BufferedOutputStream, DataOutputStream}
import java.net.Socket
import java.nio.channels.{ByteChannel, WritableByteChannel}
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

/**
 * this class is the implementation of [[PacketChannel]] and [[PacketChannelManager]]
 *
 * @param socket the socket where packets will be sent
 * @param taskID the taskID attributed to this PacketChannel
 *
 * @see [[PacketChannel]]
 * @see [[PacketChannelManager]]
 * */
class SimplePacketChannel(private val socket: Socket,
                          override val taskID: Int)
        extends PacketChannel with PacketChannelManager {

    private val out = new BufferedOutputStream(socket.getOutputStream)

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
        val bytes = new DataPacket(taskID, header, content).toBytes
        out.write(bytes)
        out.flush()
    }
    //TODO doc
    override def sendInitPacket(initInfo: TaskInitInfo): Unit = {
        val packet = TaskInitPacket.of(taskID, initInfo)
        //println("SENDING : " + packet.toBytes.array().mkString("Array(", ", ", ")"))
        //println("SENDING (asString): " + new String(packet.toBytes.array()))
        out.write(packet.toBytes)
        out.flush()
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