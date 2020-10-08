package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

/**
 * this class is used to represent a packet to send or to receive.
 * It allows the user and the program to work easier with the packets.
 * a DataPacket can only be send into tasks
 *
 * @param taskID  the task identifier where this packet comes from / goes to
 *                (take a look at [[PacketChannel]] and [[fr.overridescala.vps.ftp.api.task.TasksHandler]])
 * @param header  the header of the packet, or the type of this data. Headers allows to classify packets / data to send or receive
 * @param content the content of this packet. can be an [[Object]], a [[String]] or whatever. default content is empty
 * */
case class DataPacket(override val taskID: Int,
                      header: String,
                      override val targetIdentifier: String,
                      override val senderIdentifier: String,
                      override val content: Array[Byte] = Array()) extends Packet {

  /**
   * Represents this packet as a String
   * */
  override def toString: String =
    s"DataPacket{id: $taskID, header: $header, target: $targetIdentifier, sender: $senderIdentifier, content: ${new String(content)}}"

  object DataPacketFactory extends PacketFactory[DataPacket]  {

    private val TYPE = "[data]".getBytes
    private val TARGET = "<target>".getBytes
    private val SENDER = "<sender_id>".getBytes
    private val HEADER = "<header>".getBytes
    private val CONTENT = "<content>".getBytes

    override def toBytes(packet: DataPacket): Array[Byte] = {
      val idBytes = String.valueOf(packet.taskID).getBytes
      val headerBytes = packet.header.getBytes
      val targetIdBytes = packet.targetIdentifier.getBytes
      val senderIdBytes = packet.senderIdentifier.getBytes
      TYPE ++ idBytes ++
        TARGET ++ targetIdBytes ++
        SENDER ++ senderIdBytes ++
        HEADER ++ headerBytes ++
        CONTENT ++ packet.content
    }

    override def canTransform(bytes: Array[Byte]): Boolean =
      bytes.startsWith(TYPE)

    override def toPacket(bytes: Array[Byte]): DataPacket = {
      val taskID = cutString(bytes, TYPE, TARGET).toInt
      val targetID = cutString(bytes, TARGET, SENDER)
      val senderID = cutString(bytes, SENDER, HEADER)
      val header = cutString(bytes, HEADER, CONTENT)
      val content = bytes.slice(bytes.indexOf(CONTENT) + CONTENT.length, bytes.length)
      DataPacket(taskID, header, targetID, senderID, content)
    }

  }

}
