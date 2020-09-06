package fr.overridescala.vps.ftp.api.utils

import java.nio.ByteBuffer
import java.util

import fr.overridescala.vps.ftp.api.packet.TaskPacket
import fr.overridescala.vps.ftp.api.task.TaskType

object Protocol {

    private val TYPE = "<type>".getBytes
    private val HEADER = "<header>".getBytes
    private val CONTENT = "<content>".getBytes
    private val END = "<end>".getBytes


    def getTaskType(bytes: Array[Byte]): TaskType = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, TYPE) + TYPE.length, indexOf(bytes, HEADER))
        TaskType.valueOf(new String(cutBytes))
    }

    def getTaskHeader(bytes: Array[Byte]): String = {
        val cutBytes = util.Arrays.copyOfRange(bytes, indexOf(bytes, HEADER) + HEADER.length, indexOf(bytes, CONTENT))
        new String(cutBytes)
    }

    def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, CONTENT) + CONTENT.length, bytes.length - END.length)
    }

    def createTaskPacket(taskType: TaskType, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val typeBytes = taskType.name().getBytes
        val headerBytes = header.getBytes

        val bytes = TYPE ++ typeBytes ++ HEADER ++ headerBytes ++ CONTENT ++ content ++ END
        ByteBuffer.wrap(bytes)
    }

    def toPacket(bytes: Array[Byte]): TaskPacket = {
        val taskType = getTaskType(bytes)
        val header = getTaskHeader(bytes)
        val content = getTaskContent(bytes)
        new TaskPacket(taskType, header, content)
    }

    private def indexOf(a: Array[Byte], b: Array[Byte]): Int = {
        val aRange = a.indices
        val bRange = b.indices
        for (i <- aRange) {
            if (loop(i))
                return i
        }

        def loop(index: Int): Boolean = {
            if (index + b.length > a.length)
                return false
            for (i <- bRange) {
                if (a(index + i) != b(i)) {
                    return false
                }
            }
            true
        }

        -1
    }


}