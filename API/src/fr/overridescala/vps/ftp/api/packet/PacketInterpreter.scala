package fr.overridescala.vps.ftp.api.packet

import scala.collection.mutable.ListBuffer

class PacketInterpreter {

    private val actions = ListBuffer.empty[Packet => Unit]

    def interpret(packet: Packet): Unit = {
        if (!packet.isFree)
            return
        for (action <- actions)
            action(packet)
    }

    def onFreePacketReceived(action: Packet => Unit): Unit = {
        actions += action
    }
}

