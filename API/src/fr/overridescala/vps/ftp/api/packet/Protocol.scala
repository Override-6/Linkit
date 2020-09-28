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
     * those flags enable to separate fields of packet;
     * <b>
     *     BEGIN <-> taskID <-> HEADER <-> header <-> CONTENT <-> content <-> END
     * */
    private val DATA_PACKET_TYPE = "[data]".getBytes
    private val DP_HEADER: Array[Byte] = "<header>".getBytes

    private val TASK_INIT_PACKET_TYPE = "[task_init]".getBytes
    private val TIP_TASK_TYPE = "<task_type>".getBytes
    private val TIP_TARGET_ID = "<target_id>".getBytes

    private val CONTENT: Array[Byte] = "<content>".getBytes
    private val END: Array[Byte] = "<end>".getBytes

    /**
     * build a [[ByteBuffer]] containing the bytes of a packet from the parameters:
     *
     * @param taskID the task id from which this packet come from
     * @param header the packet header
     * @param content the packet content
     * */
    def createDataPacket(taskID: Int, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val idBytes = String.valueOf(taskID).getBytes
        val headerBytes = header.getBytes
        val bytes = DATA_PACKET_TYPE ++ idBytes ++
                DP_HEADER ++ headerBytes ++
                CONTENT ++ content ++ END
        ByteBuffer.allocateDirect(bytes.length)
                .put(bytes)
                .flip()
    }

    def createTaskInitPacket(taskID: Int, taskType: String, targetID: String, additionalContent: Array[Byte] = Array()): ByteBuffer = {
        val taskIDBytes = String.valueOf(taskID).getBytes
        val typeBytes = taskType.getBytes
        val targetIdBytes = targetID.getBytes
        val bytes =
            TASK_INIT_PACKET_TYPE ++ taskIDBytes ++
                    TIP_TARGET_ID ++ targetIdBytes ++
                    TIP_TASK_TYPE ++ typeBytes ++
                    CONTENT ++ additionalContent ++ END
        ByteBuffer.allocateDirect(bytes.length)
                .put(bytes)
                .flip()
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

    def toDP(bytes: Array[Byte]): DataPacket = {
        val taskID = cut(bytes, DATA_PACKET_TYPE, DP_HEADER).toInt
        val header = cut(bytes, DP_HEADER, CONTENT)
        val content = cut(bytes, CONTENT, END).getBytes
        DataPacket(taskID, header, content)
    }

    private def toTIP(bytes: Array[Byte]): TaskInitPacket = {
        val taskID = cut(bytes, TASK_INIT_PACKET_TYPE, TIP_TARGET_ID).toInt
        val targetID = cut(bytes, TIP_TARGET_ID, TIP_TASK_TYPE)
        val taskType = cut(bytes, TIP_TASK_TYPE, CONTENT)
        val content = cut(bytes, CONTENT, END).getBytes
        TaskInitPacket(taskID, targetID, taskType, content)
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
    protected[packet] def containsPacket(bytes: Array[Byte]): Boolean =
        getFirstPacketLength(bytes) > 0

    /**
     * @return the content of the packet
     * @param bytes the sequence to find first content field
     * */
    private def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, CONTENT) + CONTENT.length, indexOf(bytes, END))
    }

    private def cut(src: Array[Byte], a: Array[Byte], b: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(src, indexOf(src, a) + a.length, indexOf(src, b))
        new String(cutBytes)
    }

    private def getPacketType(bytes: Array[Byte]): PacketType = {
        if (bytes.containsSlice(DATA_PACKET_TYPE))
            PacketType.DATA_PACKET
        if (bytes.containsSlice(TASK_INIT_PACKET_TYPE))
            PacketType.TASK_INIT_PACKET
        else throw new IllegalArgumentException("this byte array do not have any packet type field")
    }

    private def indexOfBegin(src: Array[Byte]): Int = {
        val index = indexOf(src, DATA_PACKET_TYPE)
        if (index == -1)
            indexOf(src, TASK_INIT_PACKET_TYPE)
        else index
    }

    private def indexOf(src: Array[Byte], toFind: Array[Byte]): Int = {
        src.toSeq.indexOfSlice(toFind.toSeq)
    }


    private class PacketType private ()
    private object PacketType {
        val DATA_PACKET: PacketType = new PacketType
        val TASK_INIT_PACKET: PacketType = new PacketType
    }

}
