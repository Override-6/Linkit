package fr.`override`.linkit.core.connection.packet.fundamental

import fr.`override`.linkit.skull.connection.packet.Packet

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

