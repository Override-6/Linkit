package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet
/**
 * Represents a packet with no specific data.
 * */
object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    override def toString: String = "EmptyPacket"

}
