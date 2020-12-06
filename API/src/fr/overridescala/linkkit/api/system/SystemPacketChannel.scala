package fr.overridescala.linkkit.api.system

import fr.overridescala.linkkit.api.packet.TrafficHandler
import fr.overridescala.linkkit.api.packet.channel.SyncPacketChannel
import fr.overridescala.linkkit.api.system.SystemPacketChannel.SystemChannelID

class SystemPacketChannel(connectedID: String,
                          traffic: TrafficHandler)
        extends SyncPacketChannel(connectedID, SystemChannelID, traffic) {


    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel {
    val SystemChannelID = 6
}
