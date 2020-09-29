package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

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
     * @return true if this packet contains content, false instead
     * */
    lazy val haveContent: Boolean = !content.isEmpty

    /**
     * the packet represented to bytes sequence.
     * */
    def toBytes: ByteBuffer

}
