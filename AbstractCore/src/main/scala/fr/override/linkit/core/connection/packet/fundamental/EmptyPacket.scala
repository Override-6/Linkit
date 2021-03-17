package fr.`override`.linkit.core.connection.packet.fundamental

import fr.`override`.linkit.skull.connection.packet.Packet
/**
 * Represents a packet with no specific data.
 * */
object EmptyPacket extends Packet {
    type EmptyPacket = EmptyPacket.type

    override def toString: String = "EmptyPacket"

}
