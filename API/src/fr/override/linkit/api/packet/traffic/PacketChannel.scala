package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.system.JustifiedCloseable

/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannel]]
 * */
trait PacketChannel extends JustifiedCloseable {

    val ownerID: String
    val traffic: PacketTraffic
    val identifier: Int

}
