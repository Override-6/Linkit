package fr.`override`.linkit.api.packet.factory

import fr.`override`.linkit.api.packet.TrafficHandler
import fr.`override`.linkit.api.packet.channel.PacketChannel

trait PacketChannelFactory[C <: PacketChannel] {

    def createNew(trafficHandler: TrafficHandler, channelId: Int, targetIdentifier: String): C

}
