package fr.overridescala.vps.ftp.api.packet

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 * The type of packets ([[TaskInitPacket]] & [[DataPacket]]) is determined by [[Protocol]]
 *
 * */
case class TaskInitPacket(override val taskID: Int,
                          targetId: String,
                          taskType: String,
                          override val content: Array[Byte] = Array()) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskID: $taskID, taskType: $taskType, targetId: $targetId, additionalContent: ${new String(content)}}"

}
