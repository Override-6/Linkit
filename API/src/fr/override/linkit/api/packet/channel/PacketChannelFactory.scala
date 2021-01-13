package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.traffic.PacketTraffic

trait PacketChannelFactory[C <: PacketChannel] {

    final val factory = this //For Java users
    val channelClass: Class[C]

    def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): C
}
