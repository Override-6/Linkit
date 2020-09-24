package fr.overridescala.vps.ftp.api.packet

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
case class DataPacket private[packet](taskID: Int,
                 header: String,
                 content: Array[Byte] = Array()) {

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    /**
     * Represents this packet as a String
     * */
    override def toString: String =
        s"TaskPacket{id: $taskID, header: $header, content: ${new String(content)}}"

}
