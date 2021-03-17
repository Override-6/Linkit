package fr.`override`.linkit.core.connection.packet.fundamental

import fr.`override`.linkit.skull.connection.packet.Packet

case class WrappedPacket(tag: String, subPacket: Packet) extends Packet

