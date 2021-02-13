package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    override def toString: String = "EmptyPacket"

}
