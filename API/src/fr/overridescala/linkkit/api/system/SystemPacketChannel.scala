package fr.overridescala.linkkit.api.system

import fr.overridescala.linkkit.api.packet.{PacketChannelsHandler, SyncPacketChannel}
import fr.overridescala.linkkit.api.system.SystemPacketChannel.SystemChannelID

class SystemPacketChannel(connectedID: String,
                          ownerID: String,
                          handler: PacketChannelsHandler)
        extends SyncPacketChannel(connectedID, ownerID, SystemChannelID, handler) {

    private val notifier = handler.notifier

    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        notifier.onSystemOrderSent(systemOrder)
    }

}

object SystemPacketChannel {
    val SystemChannelID = 6
}
