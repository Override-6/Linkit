package fr.overridescala.vps.ftp.api.packet

trait Packet {

    /**
     * the task identifier that determine where the packet have to go / from what kind of task he comes from
     * */
    val taskID: Int
    /**
     * the data content
     * */
    val content: Array[Byte] = Array()

    /**
     * the targeted Relay identifier that will receive the packet
     * */
    val targetIdentifier: String

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    /**
     * the packet represented to bytes sequence.
     * */
    implicit def toBytes: Array[Byte]


}

object Packet {
    implicit def autoBytes(packet: Packet): Array[Byte] =
        packet.toBytes
}
