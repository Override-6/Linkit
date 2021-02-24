package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.Packet

case class WriterInfo(traffic: PacketTraffic, identifier: Int, transform: Packet => Packet)
