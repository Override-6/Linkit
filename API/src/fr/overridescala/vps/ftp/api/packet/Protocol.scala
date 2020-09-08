package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer
import java.util

object Protocol {

    private val SESSION = "<session_id>".getBytes
    private val HEADER = "<header>".getBytes
    private val CONTENT = "<content>".getBytes

    def getSessionID(bytes: Array[Byte]): Int = {
        val cutBytes = util.Arrays.copyOfRange(bytes, SESSION.length, indexOf(bytes, HEADER))
        new String(cutBytes).toInt
    }

    def getTaskHeader(bytes: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, HEADER) + HEADER.length, indexOf(bytes, CONTENT))
        new String(cutBytes)
    }

    def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, CONTENT) + CONTENT.length, bytes.length)
    }

    def createTaskPacket(sessionID: Int, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val headerBytes = header.getBytes
        val id = String.valueOf(sessionID).getBytes
        val bytes = SESSION ++ id ++ HEADER ++ headerBytes ++ CONTENT ++ content
        ByteBuffer.wrap(bytes)
    }

    def toPacket(bytes: Array[Byte]): DataPacket = {
        val sessionID = getSessionID(bytes)
        val header = getTaskHeader(bytes)
        val content = getTaskContent(bytes)
        new DataPacket(sessionID, header, content)
    }

    private def indexOf(a: Array[Byte], b: Array[Byte]): Int = {
        a.toSeq.indexOfSlice(b.toSeq)
    }


}
