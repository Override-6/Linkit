package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer
import java.util

object Protocol {

    protected val BEGIN: Array[Byte] = "<begin>".getBytes
    protected val HEADER: Array[Byte] = "<header>".getBytes
    protected val CONTENT: Array[Byte] = "<content>".getBytes
    protected val END: Array[Byte] = "<end>".getBytes

    def createTaskPacket(sessionID: Int, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val headerBytes = header.getBytes
        val id = String.valueOf(sessionID).getBytes
        val bytes = BEGIN ++ id ++ HEADER ++ headerBytes ++ CONTENT ++ content ++ END
        ByteBuffer.wrap(bytes)
    }

    /**
     * creates a packet from a byte Array.
     *
     * @return Tuple of DataPacket and his length in the array
     * */
    protected[packet] def toPacket(bytes: Array[Byte]): DataPacket = {
        val sessionID = getSessionID(bytes)
        val header = getTaskHeader(bytes)
        val content = getTaskContent(bytes)
        new DataPacket(sessionID, header, content)
    }

    protected[packet] def getPacketLength(bytes: Array[Byte]): Int = {
        val begin = indexOf(bytes, BEGIN)
        val end = indexOf(bytes, END) + END.length
        //fast check
        if (begin == -1 || end == -1) {
            throw new IllegalArgumentException("this byte sequence does not contains valid packet !")
        }
        end - begin
    }

    protected[packet] def containsPacket(bytes: Array[Byte]): Boolean = {
        val beginIndex = indexOf(bytes, BEGIN)
        lazy val headerIndex = indexOf(bytes, HEADER)
        lazy val contentIndex = indexOf(bytes, CONTENT)
        lazy val endIndex = indexOf(bytes, END)

        beginIndex != -1 && headerIndex > beginIndex && contentIndex > headerIndex && endIndex > contentIndex
    }

    private def getSessionID(bytes: Array[Byte]): Int = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, BEGIN) + BEGIN.length, indexOf(bytes, HEADER))
        new String(cutBytes).toInt
    }

    private def getTaskHeader(bytes: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, HEADER) + HEADER.length, indexOf(bytes, CONTENT))
        new String(cutBytes)
    }

    private def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, CONTENT) + CONTENT.length, indexOf(bytes, END))
    }

    private def indexOf(src: Array[Byte], toFind: Array[Byte]): Int = {
        src.toSeq.indexOfSlice(toFind.toSeq)
    }

}
