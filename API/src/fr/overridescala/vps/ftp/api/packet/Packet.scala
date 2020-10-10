package fr.overridescala.vps.ftp.api.packet

//TODO Doc
trait Packet {

    /**
     * note: channelID == taskID
     * */
    val channelID: Int

    /**
     * the data content
     * */
    val content: Array[Byte] = Array()

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    lazy val className: String = getClass.getSimpleName


    /**
     * the targeted Relay identifier that will receive the packet
     * */
    val targetIdentifier: String

    /**
     * this packet sender identifier
     * */
    val senderIdentifier: String

}