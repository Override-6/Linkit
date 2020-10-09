package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.{IdentifiablePacket, Packet}
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
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
                                  override val content: Array[Byte] = Array()) extends IdentifiablePacket {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $taskID, taskType: $taskType, target: $targetIdentifier, sender: $senderIdentifier, additionalContent: ${new String(content)}}"


}

object TaskInitPacket {
    def of(senderID: String, taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, senderID, info.taskType, info.content)

    object Factory extends PacketFactory[TaskInitPacket] {

        private val TYPE = "[task_init]".getBytes
        private val TARGET = "<target>".getBytes
        private val SENDER = "<sender_id>".getBytes
        private val TASK_TYPE = "<task_type>".getBytes
        private val CONTENT = "<content>".getBytes

        override def toBytes(implicit packet: TaskInitPacket): Array[Byte] = {
            val taskIDBytes = String.valueOf(packet.taskID).getBytes
            val typeBytes = packet.taskType.getBytes
            val targetIdBytes = packet.targetIdentifier.getBytes
            val senderIdBytes = packet.senderIdentifier.getBytes
            TYPE ++ taskIDBytes ++
                    TARGET ++ targetIdBytes ++
                    SENDER ++ senderIdBytes ++
                    TASK_TYPE ++ typeBytes ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): TaskInitPacket = {
            val taskID = cutString(TYPE, TARGET).toInt
            val targetID = cutString(TARGET, SENDER)
            val senderID = cutString(SENDER, TASK_TYPE)
            val taskType = cutString(TASK_TYPE, CONTENT)
            val content = cutEnd(CONTENT)
            TaskInitPacket(taskID, targetID, senderID, taskType, content)
        }

    }
}
