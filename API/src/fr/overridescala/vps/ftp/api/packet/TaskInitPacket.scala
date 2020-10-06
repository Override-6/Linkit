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
                                  override val senderIdentifier: String,
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
        s"TaskInitPacket{taskID: $taskID, taskType: $taskType, target: $targetIdentifier, sender: $senderIdentifier, additionalContent: ${new String(content)}}"
}

object TaskInitPacket {
    def of(senderID: String, taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, senderID, info.taskType, info.content)
}
