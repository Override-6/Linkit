package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

/**
 * this class is used by [[fr.overridescala.vps.ftp.api.Relay]] and [[fr.overridescala.vps.ftp.api.task.TasksHandler]]
 * to add packets into a PacketChannel, or send special packet such as [[TaskInitPacket]]
 * */
trait PacketChannelManager {

    /**
     * add a packet into the PacketChannel. the PacketChannel will stop waiting in [[PacketChannel#nextPacket]] if it where waiting for a packet
     *
     * @param packet the packet to add
     * @throws UnexpectedPacketException if the packet id not equals the channel task ID
     * */
    def addPacket(packet: DataPacket): Unit
    //TODO doc

    /**
     * Send a packet that will be used to determine the correct Task completer used by the targeted relay
     * */
    def sendInitPacket(initInfo: TaskInitInfo): Unit

    /**
     * the identifier of this task
     * */
    val taskID: Int

}