package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.traffic.dedicated.SyncPacketChannel

class SystemPacketChannel(writer: PacketWriter, connectedID: String)
        extends SyncPacketChannel(writer, connectedID, true) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        sendPacket(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}
