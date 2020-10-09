package fr.overridescala.vps.ftp.api.packet

trait Packet {

    /**
     * the data content
     * */
    val content: Array[Byte] = Array()

    /**
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    lazy val className: String = getClass.getSimpleName

}