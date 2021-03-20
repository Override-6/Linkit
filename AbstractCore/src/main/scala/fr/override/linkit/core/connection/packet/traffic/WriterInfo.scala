package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic

case class WriterInfo(traffic: PacketTraffic,
                      identifier: Int,
                      transform: Packet => Packet)
