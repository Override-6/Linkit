package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 * The type of packets ([[TaskInitPacket]] & [[DataPacket]]) is determined by [[Protocol]]
 * @see [[Packet]]
 * */
case class TaskInitPacket private(override val channelID: Int,
                                  override val targetIdentifier: String,
                                  override val senderIdentifier: String,
                                  taskType: String,
                                  override val content: Array[Byte] = Array()) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $channelID, taskType: $taskType, target: $targetIdentifier, sender: $senderIdentifier, additionalContent: ${new String(content)}}"


}

object TaskInitPacket {
    def of(senderID: String, taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, senderID, info.taskType, info.content)

    object Factory extends PacketFactory[TaskInitPacket] {

        private val TYPE = "[task_init]".getBytes
        private val TASK_TYPE = "<task_type>".getBytes
        private val CONTENT = "<content>".getBytes

        override def toBytes(implicit packet: TaskInitPacket): Array[Byte] = {
            val typeBytes = packet.taskType.getBytes
            TYPE ++ EmptyPacket.Factory.toBytesUnsigned(packet)
                    TASK_TYPE ++ typeBytes ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): TaskInitPacket = {
            val base = EmptyPacket.Factory.toPacket(bytes)
            val taskType = cutString(TASK_TYPE, CONTENT)
            val content = cutEnd(CONTENT)
            TaskInitPacket(base.channelID, base.targetIdentifier, base.senderIdentifier, taskType, content)
        }

    }
}
