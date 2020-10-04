package fr.overridescala.vps.ftp.api.packet
import java.nio.ByteBuffer

/**
 * this class is used to represent a packet to send or to receive.
 * It allows the user and the program to work easier with the packets.
 * a DataPacket can only be send into tasks
 *
 * @param taskID the task identifier where this packet comes from / goes to
 *               (take a look at [[PacketChannel]] and [[fr.overridescala.vps.ftp.api.task.TasksHandler]])
 * @param header the header of the packet, or the type of this data. Headers allows to classify packets / data to send or receive
 * @param content the content of this packet. can be an [[Object]], a [[String]] or whatever. default content is empty
 * */
class DataPacket private[packet](override val taskID: Int,
                                      val header: String,
                                      override val content: Array[Byte] = Array()) extends Packet {

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"DataPacket{id: $taskID, header: $header, content: ${new String(content)}}"

    /**
     * the packet represented to bytes sequence.
     * */
    override def toBytes: Array[Byte] = Protocol.toBytes(this)

    override def equals(obj: Any): Boolean = {
        if (!obj.isInstanceOf[DataPacket])
            return false
        val packet = obj.asInstanceOf[DataPacket]
        taskID.equals(packet.taskID) && header.equals(packet.header) && content.sameElements(packet.content)
    }
}
