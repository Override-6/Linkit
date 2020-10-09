package fr.overridescala.vps.ftp.api.packet

trait IdentifiablePacket extends Packet {


    /**
     * the task identifier that determine where the packet have to go / from what kind of task he comes from
     * */
    val taskID: Int

    /**
     * the targeted Relay identifier that will receive the packet
     * */
    val targetIdentifier: String

    /**
     * this packet sender identifier
     * */
    val senderIdentifier: String

}
