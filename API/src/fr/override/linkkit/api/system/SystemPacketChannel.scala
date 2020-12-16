package fr.`override`.linkkit.api.system

import fr.`override`.linkkit.api.packet.TrafficHandler
import fr.`override`.linkkit.api.packet.TrafficHandler
import fr.`override`.linkkit.api.packet.channel.SyncPacketChannel
import fr.`override`.linkkit.api.system.SystemPacketChannel.SystemChannelID

class SystemPacketChannel(connectedID: String,
                          traffic: TrafficHandler)
        extends SyncPacketChannel(connectedID, SystemChannelID, 128, traffic) {


    def sendOrder(systemOrder: SystemOrder, reason: Reason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel {
    val SystemChannelID = 6
}
