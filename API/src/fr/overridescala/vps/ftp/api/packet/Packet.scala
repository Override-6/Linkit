package fr.overridescala.vps.ftp.api.packet

trait Packet {

    val taskID: Int
    val content: Array[Byte] = Array()


    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    def toString: String

}
