package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.dedicated.SyncPacketChannel

class SystemPacketChannel(connectedID: String,
                          traffic: PacketTraffic)
        extends SyncPacketChannel(connectedID, PacketTraffic.SystemChannel, traffic, true) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}
