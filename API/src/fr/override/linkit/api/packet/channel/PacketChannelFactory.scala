package fr.`override`.linkit.api.packet.channel

import fr.`override`.linkit.api.packet.traffic.PacketWriter

trait PacketChannelFactory[C <: PacketChannel] {

    final val factory = this //For Java users
    val channelClass: Class[C]

    def createNew(writer: PacketWriter, channelId: Int, connectedID: String): C
}
