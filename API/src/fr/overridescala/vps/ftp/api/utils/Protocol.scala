package fr.overridescala.vps.ftp.api.utils

import java.nio.ByteBuffer
import java.util

import fr.overridescala.vps.ftp.api.packet.TaskPacket
import fr.overridescala.vps.ftp.api.task.TaskType

import scala.util.control.Breaks.break

object Protocol {

    private val TYPE = "<type>".getBytes
    private val HEADER = "<header>".getBytes
    private val CONTENT = "<content>".getBytes
    private val END = "<end>".getBytes


    def getTaskType(bytes: Array[Byte]): TaskType = {
        val name = new String(between(bytes, TYPE, HEADER))
        TaskType.valueOf(name)
    }

    def getTaskHeader(bytes: Array[Byte]): String = {
        new String(between(bytes, HEADER, CONTENT))
    }

    def getTaskContent(bytes: Array[Byte]): Array[Byte] = {
        between(bytes, CONTENT, END)
    }

    def createTaskPacket(taskType: TaskType, header: String, content: Array[Byte] = Array()): ByteBuffer = {
        val typeBytes = taskType.name().getBytes
        val headerBytes = header.getBytes

        val bytes = TYPE ++ typeBytes ++ HEADER ++ headerBytes ++ CONTENT ++ content ++ END
        ByteBuffer.wrap(bytes)
    }

    def toPacket(buffer: ByteBuffer): TaskPacket = {
        val bytes = buffer.array()
        val taskType = getTaskType(bytes)
        val header = getTaskHeader(bytes)
        val content = getTaskContent(bytes)
        new TaskPacket(taskType, header, content)
    }

    private def between(bytes: Array[Byte], a: Array[Byte], b: Array[Byte]): Array[Byte] = {
        util.Arrays.copyOfRange(bytes, indexOf(bytes, a) + a.length, indexOf(bytes, b))
    }

    private def indexOf(a: Array[Byte], b: Array[Byte]): Int = {
        for (i <- a.indices) {
            if (loop(i))
                return i
        }

        def loop(index: Int): Boolean = {
            for (i <- b.indices) {
                if (a(index + i) != b(i)) {
                    return false
                }
            }
            true
        }

        -1
    }


}
