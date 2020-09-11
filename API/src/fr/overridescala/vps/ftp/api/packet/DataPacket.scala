package fr.overridescala.vps.ftp.api.packet

class DataPacket(protected[api] val sessionID: Int,
                 val header: String,
                 val content: Array[Byte] = Array()) {


    lazy val haveContent: Boolean = !content.isEmpty

    override def toString: String =
        s"TaskPacket{id: $sessionID, header: $header, content: ${new String(content)}}"

}
