package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer
import java.util

/**
 * Protocol describes how the packets are interpreted and build.
 *
 * <b>/!\ this is not a serious protocol/!\</b>
 * */
object Protocol {
    /**
     * those flags enable to separate fields of packet, and packets type
     * */
    private val DATA_PACKET_TYPE = "[data]".getBytes
    private val DP_HEADER: Array[Byte] = "<header>".getBytes

    private val TASK_INIT_PACKET_TYPE = "[task_init]".getBytes
    private val TIP_TASK_TYPE = "<task_type>".getBytes
    private val TIP_TARGET_ID = "<target_id>".getBytes

    private val CONTENT: Array[Byte] = "<content>".getBytes
    private val END: Array[Byte] = "<end>".getBytes

    val INIT_ID: Int = -1
    val ERROR_ID: Int = -2

    val ABORT_TASK_PACKET: DataPacket = DataPacket(ERROR_ID, "ABORT_TASK")

    /**
     * build a [[ByteBuffer]] containing the bytes of a packet from the parameters:
     *
     * @param taskID the task id from which this packet come from
     * @param header the packet header
     * @param content the packet content
     * */
    def toBytes(packet: DataPacket): ByteBuffer = {
        val idBytes = String.valueOf(packet.taskID).getBytes
        val headerBytes = packet.header.getBytes
        val bytes = DATA_PACKET_TYPE ++ idBytes ++
                DP_HEADER ++ headerBytes ++
                CONTENT ++ packet.content ++ END
        ByteBuffer.wrap(bytes)
    }

    def toBytes(packet: TaskInitPacket): ByteBuffer = {
        val taskIDBytes = String.valueOf(packet.taskID).getBytes
        val typeBytes = packet.taskType.getBytes
        val targetIdBytes = packet.targetId.getBytes
        val bytes =
            TASK_INIT_PACKET_TYPE ++ taskIDBytes ++
                    TIP_TARGET_ID ++ targetIdBytes ++
                    TIP_TASK_TYPE ++ typeBytes ++
                    CONTENT ++ packet.content ++ END
        ByteBuffer.wrap(bytes)
    }

    private def toTIP(bytes: Array[Byte]): TaskInitPacket = {
        val taskID = cut(bytes, TASK_INIT_PACKET_TYPE, TIP_TARGET_ID).toInt
        val targetID = cut(bytes, TIP_TARGET_ID, TIP_TASK_TYPE)
        val taskType = cut(bytes, TIP_TASK_TYPE, CONTENT)
        val content = cut(bytes, CONTENT, END).getBytes
        TaskInitPacket(taskID, targetID, taskType, content)
    }

    /**
     * creates a [[DataPacket]] from a byte Array.
     *
     * @param bytes the bytes to transform
     * @return a [[DataPacket]] instance
     * @throws IllegalArgumentException if no packet where found.
     * */
    protected[packet] def toPacket(bytes: Array[Byte]): Packet = {
        getPacketType(bytes) match {
            case PacketType.TASK_INIT_PACKET => toTIP(bytes)
            case PacketType.DATA_PACKET => toDP(bytes)
        }
    }

    private def toDP(bytes: Array[Byte]): DataPacket = {
        val taskID = cut(bytes, DATA_PACKET_TYPE, DP_HEADER).toInt
        val header = cut(bytes, DP_HEADER, CONTENT)
        val content = cut(bytes, CONTENT, END).getBytes
        DataPacket(taskID, header, content)
    }


    /**
     * @return the length in the array of the first packet found, -1 if the array do not contains any packet
     * @param bytes the bytes to find first packet length
     * */
    protected[packet] def getFirstPacketLength(bytes: Array[Byte]): Int = {
        val begin = indexOfBegin(bytes)
        val end = indexOf(bytes, END) + END.length
        //fast check
        if (begin == -1 || end == -1)
            return -1
        end - begin
    }

    /**
     * @return true if this sequence contains a packet
     * @param bytes the sequence to test
     * */
    protected[packet] def containsPacket(bytes: Array[Byte]): Boolean = {
        getFirstPacketLength(bytes) > 0
    }

    private def cut(src: Array[Byte], a: Array[Byte], b: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(src, indexOf(src, a) + a.length, indexOf(src, b))
        new String(cutBytes)
    }

    private def getPacketType(bytes: Array[Byte]): PacketType = {
        if (bytes.containsSlice(DATA_PACKET_TYPE))
            PacketType.DATA_PACKET
        else if (bytes.containsSlice(TASK_INIT_PACKET_TYPE))
            PacketType.TASK_INIT_PACKET
        else throw new IllegalArgumentException("this byte array do not have any packet type field")
    }

    private def indexOfBegin(src: Array[Byte]): Int = {
        var index = indexOf(src, DATA_PACKET_TYPE)
        if (index == -1)
            index = indexOf(src, TASK_INIT_PACKET_TYPE)
        index
    }

    private def indexOf(src: Array[Byte], toFind: Array[Byte]): Int = {
        src.toSeq.indexOfSlice(toFind.toSeq)
    }


    private class PacketType private()

    private object PacketType extends Enumeration {
        val DATA_PACKET: PacketType = new PacketType
        val TASK_INIT_PACKET: PacketType = new PacketType
    }

}
