package fr.`override`.linkit.skull.connection.packet.traffic

import fr.`override`.linkit.skull.connection.packet.Packet

trait PacketSender extends PacketChannel {

    def send(packet: Packet): Unit

    @throws[IllegalArgumentException]("If targets contains an identifier that is not authorised by his scope.")
    def sendTo(packet: Packet, targets: String*): Unit

}
