package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

case class WrappedPacket(tag: String, subPacket: Packet) extends Packet

