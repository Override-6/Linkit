package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 * @see [[Packet]]
 * */
case class TaskInitPacket private(override val channelID: Int,
                                  override val targetID: String,
                                  override val senderID: String,
                                  taskType: String,
                                  override val content: Array[Byte] = Array()) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $channelID, taskType: $taskType, target: $targetID, sender: $senderID, additionalContent: ${new String(content)}}"


}

object TaskInitPacket {
    def of(senderID: String, taskId: Int, info: TaskInitInfo): TaskInitPacket =
        TaskInitPacket(taskId, info.targetID, senderID, info.taskType, info.content)

    object Factory extends PacketFactory[TaskInitPacket] {

        private val TYPE = "[task_init]".getBytes
        private val SENDER = "<sender>".getBytes
        private val TARGET = "<target>".getBytes
        private val TASK_TYPE = "<task_type>".getBytes
        private val CONTENT = "<content>".getBytes

        override def decompose(implicit packet: TaskInitPacket): Array[Byte] = {
            val channelID = packet.channelID.toString.getBytes
            val sender = packet.senderID.getBytes
            val target = packet.targetID.getBytes
            val typeBytes = packet.taskType.getBytes
            TYPE ++ channelID ++
                    SENDER ++ sender ++
                    TARGET ++ target ++
                    TASK_TYPE ++ typeBytes ++
                    CONTENT ++ packet.content
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def build(implicit bytes: Array[Byte]): TaskInitPacket = {
            val channelID = cutString(TYPE, SENDER).toInt
            val sender = cutString(SENDER, TARGET)
            val target = cutString(TARGET, TASK_TYPE)
            val taskType = cutString(TASK_TYPE, CONTENT)
            val content = cutEnd(CONTENT)
            TaskInitPacket(channelID, target, sender, taskType, content)
        }

    }

}
