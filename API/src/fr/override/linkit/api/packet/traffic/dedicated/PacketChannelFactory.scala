package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.traffic.{DedicatedPacketInjectable, PacketTraffic}

trait PacketChannelFactory[C <: PacketChannel] {

    final val factory: this.type = this //For Java users

    val channelClass: Class[C]

    def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): C with DedicatedPacketInjectable




    type T <: ChannelBehavior

    implicit def behavioralFactory(behavior: T): PacketChannelFactory[C] = this

    //an Empty behavior description
    trait ChannelBehavior {
    }
}
