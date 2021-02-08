package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.Packet

trait PacketSender extends PacketChannel {

    def send(packet: Packet): Unit

    def sendTo(target: String, packet: Packet): Unit

}
