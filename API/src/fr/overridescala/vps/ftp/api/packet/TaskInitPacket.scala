package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

import fr.overridescala.vps.ftp.api.task.TaskInitInfo

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 * The type of packets ([[TaskInitPacket]] & [[DataPacket]]) is determined by [[Protocol]]
 * @see [[Packet]]
 * */
case class TaskInitPacket(override val taskID: Int,
                          targetId: String,
                          taskType: String,
                          override val content: Array[Byte] = Array()) extends Packet {

    /**
     * the packet represented to bytes sequence.
     * */
    override def toBytes: ByteBuffer = Protocol.toBytes(this)

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $taskID, taskType: $taskType, targetId: $targetId, additionalContent: ${new String(content)}}"

    override def equals(obj: Any): Boolean = {
        if (!obj.isInstanceOf[TaskInitPacket])
            return false
        val packet = obj.asInstanceOf[TaskInitPacket]
        lazy val contentEquals = content.sameElements(packet.content)
        taskID.equals(packet.taskID) && targetId.equals(targetId) && taskType.equals(taskType) && contentEquals
    }
}

object TaskInitPacket {
    def of(taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, info.taskType, info.content)
}
