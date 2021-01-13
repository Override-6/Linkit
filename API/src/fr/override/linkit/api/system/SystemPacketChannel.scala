package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.channel.SyncPacketChannel
import fr.`override`.linkit.api.packet.traffic.PacketTraffic

class SystemPacketChannel(connectedID: String,
                          traffic: PacketTraffic)
        extends SyncPacketChannel(connectedID, PacketTraffic.SystemChannel, traffic) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}
