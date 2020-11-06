package fr.overridescala.vps.ftp.api.packet

//TODO Doc
trait Packet {

    /**
     * note: channelID == taskID
     * */
    val channelID: Int

    lazy val className: String = getClass.getSimpleName


    /**
     * the targeted Relay identifier that will receive the packet
     * */
    val targetID: String

    /**
     * this packet sender identifier
     * */
    val senderID: String

}