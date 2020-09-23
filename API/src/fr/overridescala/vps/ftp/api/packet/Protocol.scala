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
    protected val BEGIN: Array[Byte] = "<begin>".getBytes
    protected val HEADER: Array[Byte] = "<header>".getBytes
    protected val CONTENT: Array[Byte] = "<content>".getBytes
    protected val END: Array[Byte] = "<end>".getBytes

    /**
     * build a [[ByteBuffer]] containing the bytes of a packet from the parameters:
     *
     * @param taskID the task id from which this packet come from
     * @param header the packet header
     * @param content the packet content
     * */
    def createTaskPacket(taskID: Int, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val headerBytes = header.getBytes
        val id = String.valueOf(taskID).getBytes
        val bytes = BEGIN ++ id ++ HEADER ++ headerBytes ++ CONTENT ++ content ++ END
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
    protected[packet] def toPacket(bytes: Array[Byte]): DataPacket = {
        try {
            val sessionID = getTaskID(bytes)
            val header = getTaskHeader(bytes)
            val content = getTaskContent(bytes)
            DataPacket(sessionID, header, content)
        } catch {
            case e: IndexOutOfBoundsException =>
                throw new IllegalArgumentException("no packet found in this byte sequence", e)
        }
    }

    /**
     * @return the length in the array of the first packet found, -1 if the array do not contains any packet
     * @param bytes the bytes to find first packet length
     * */
    protected[packet] def getFirstPacketLength(bytes: Array[Byte]): Int = {
        val begin = indexOf(bytes, BEGIN)
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
        val beginIndex = indexOf(bytes, BEGIN)
        lazy val headerIndex = indexOf(bytes, HEADER)
        lazy val contentIndex = indexOf(bytes, CONTENT)
        lazy val endIndex = indexOf(bytes, END)

        beginIndex != -1 && headerIndex > beginIndex && contentIndex > headerIndex && endIndex > contentIndex
    }

    /**
     * @return the task ID of the packet
     * @param bytes the sequence to find first Task ID field
     * */
    private def getTaskID(bytes: Array[Byte]): Int = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, BEGIN) + BEGIN.length, indexOf(bytes, HEADER))
        new String(cutBytes).toInt
    }

    /**
     * @return the header of the packet
     * @param bytes the sequence to find first header field
     * */
    private def getTaskHeader(bytes: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, HEADER) + HEADER.length, indexOf(bytes, CONTENT))
        new String(cutBytes)
    }

    /**
     * @return the content of the packet
     * @param bytes the sequence to find first content field
     * */
    private def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, CONTENT) + CONTENT.length, indexOf(bytes, END))
    }

    private def indexOf(src: Array[Byte], toFind: Array[Byte]): Int = {
        src.toSeq.indexOfSlice(toFind.toSeq)
    }

}
