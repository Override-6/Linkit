package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.packet.{PacketChannelsHandler, SyncPacketChannel}
import fr.overridescala.vps.ftp.api.system.SystemPacketChannel.SystemChannelID
import fr.overridescala.vps.ftp.api.task.TaskInitInfo

class SystemPacketChannel(connectedID: String,
                          ownerID: String,
                          handler: PacketChannelsHandler) extends SyncPacketChannel(connectedID, ownerID, SystemChannelID, handler) {

    private val notifier = handler.notifier

    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content)(this))
        notifier.onSystemOrderSent(systemOrder)
    }

    def sendInitPacket(taskInitInfo: TaskInitInfo): Unit = {
        sendPacket(TaskInitPacket(taskInitInfo)(this))
    }

}

object SystemPacketChannel {
    val SystemChannelID = 6
}
