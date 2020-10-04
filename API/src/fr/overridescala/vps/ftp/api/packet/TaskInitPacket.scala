package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.task.TaskInitInfo

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 * The type of packets ([[TaskInitPacket]] & [[DataPacket]]) is determined by [[Protocol]]
 * @see [[Packet]]
 * */
case class TaskInitPacket private(override val taskID: Int,
                                  override val targetIdentifier: String,
                                  senderIdentifier: String,
                                  taskType: String,
                                  override val content: Array[Byte] = Array()) extends Packet {

    /**
     * the packet represented to bytes sequence.
     * */
    override def toBytes: Array[Byte] = Protocol.toBytes(this)

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $taskID, taskType: $taskType, target: $targetIdentifier, additionalContent: ${new String(content)}}"

    override def equals(obj: Any): Boolean = {
        if (!obj.isInstanceOf[TaskInitPacket])
            return false
        val packet = obj.asInstanceOf[TaskInitPacket]
        taskID == packet.taskID &&
                targetIdentifier.equals(targetIdentifier) &&
                taskType.equals(taskType) &&
                content.sameElements(packet.content)
    }
}

object TaskInitPacket {
    def of(taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, info.taskType, info.content)
}
