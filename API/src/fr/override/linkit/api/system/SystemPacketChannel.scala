package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.TrafficHandler
import fr.`override`.linkit.api.packet.TrafficHandler
import fr.`override`.linkit.api.packet.channel.SyncPacketChannel
import fr.`override`.linkit.api.system.SystemPacketChannel.SystemChannelID

class SystemPacketChannel(connectedID: String,
                          traffic: TrafficHandler)
        extends SyncPacketChannel(connectedID, SystemChannelID, 128, traffic) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel {
    val SystemChannelID = 6
}
