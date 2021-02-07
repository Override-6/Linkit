package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.traffic.{DedicatedPacketInjectable, PacketWriter}

trait PacketChannelFactory[C <: PacketChannel] {

    def createNew(writer: PacketWriter, connectedID: String): C with DedicatedPacketInjectable

}
