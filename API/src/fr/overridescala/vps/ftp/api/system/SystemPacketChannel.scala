package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.packet.{PacketChannelsHandler, SyncPacketChannel}
import SystemInfo._
import fr.overridescala.vps.ftp.api.system.SystemPacketChannel.SystemChannelID

class SystemPacketChannel(connectedID: String,
                          ownerID: String,
                          handler: PacketChannelsHandler) extends SyncPacketChannel(connectedID, ownerID, SystemChannelID, handler) {

    private val notifier = handler.notifier

    def sendOrder(systemOrder: SystemOrder): Unit = {
        handler.sendPacket(SystemPacket(systemOrder)(this))
        notifier.onSystemOrderSent(systemOrder)
    }

    def sendError(systemError: SystemError, reason: Reason): Unit = {
        handler.sendPacket(SystemPacket(systemError)(this))
        notifier.onSystemError(systemError, reason)
    }



}

object SystemPacketChannel {
    val SystemChannelID = 6
}
