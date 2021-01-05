package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.traffic.PacketSender

trait PacketChannelFactory[C <: PacketChannel] {

    final val factory = this //For Java users
    val channelClass: Class[C]

    def createNew(sender: PacketSender, channelId: Int, connectedID: String): C
}
