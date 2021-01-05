package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketFactory}

//TODO doc parameters
/**
 * this type of packet is sent when a relay ask to server to schedule a task between him, the server, and the target
 *
 * @see [[Packet]]
 * */
case class TaskInitPacket(taskType: String,
                          content: Array[Byte] = Array()) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskInitPacket{taskType: $taskType, additionalContent: ${new String(content)}}"

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty
}

object TaskInitPacket extends PacketFactory[TaskInitPacket] {

    import fr.`override`.linkit.api.packet.PacketUtils._

    private val TYPE = "[task_init]".getBytes
    private val CONTENT = "<content>".getBytes

    override def decompose(implicit packet: TaskInitPacket): Array[Byte] = {
        val typeBytes = packet.taskType.getBytes
        TYPE ++ typeBytes ++
            CONTENT ++ packet.content
    }

    override def canTransform(implicit bytes: Array[Byte]): Boolean =
        bytes.startsWith(TYPE)

    override def build(implicit bytes: Array[Byte]): TaskInitPacket = {
        val taskType = cutString(TYPE, CONTENT)
        val content = cutEnd(CONTENT)
        TaskInitPacket(taskType, content)
    }

    override val packetClass: Class[TaskInitPacket] = classOf[TaskInitPacket]
}
