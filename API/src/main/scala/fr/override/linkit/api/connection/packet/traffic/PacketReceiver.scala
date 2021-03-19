package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet

trait PacketReceiver extends PacketChannel {

    def nextPacket[P <: Packet]: P

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}