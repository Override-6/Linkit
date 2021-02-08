package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.traffic.dedicated.SyncPacketChannel
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectableFactory}

class SystemPacketChannel(scope: ChannelScope)
        extends SyncPacketChannel(scope, true) {


    def sendOrder(systemOrder: SystemOrder, reason: CloseReason, content: Array[Byte] = Array()): Unit = {
        send(SystemPacket(systemOrder, reason, content))
        //notifier.onSystemOrderSent(systemOrder) TODO rebased event system
    }

}

object SystemPacketChannel extends PacketInjectableFactory[SystemPacketChannel] {
    override def createNew(scope: ChannelScope): SystemPacketChannel = {
        new SystemPacketChannel(scope)
    }
}
