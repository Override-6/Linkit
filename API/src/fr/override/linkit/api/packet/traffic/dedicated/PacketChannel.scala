package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system.JustifiedCloseable

//TODO Think about on creating an abstract class to implement this class as a trait
/**
 * this class link two Relay between them. As a Channel, it can send packet, or wait until a packet was received
 *
 * @see [[PacketChannel]]
 * */
trait PacketChannel extends JustifiedCloseable {

    val ownerID: String
    val injector: PacketTraffic

    val connectedID: String
    val coordinates: PacketCoordinates

}