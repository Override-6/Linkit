package fr.`override`.linkit.api.packet.factory

import fr.`override`.linkit.api.packet.TrafficHandler
import fr.`override`.linkit.api.packet.collector.PacketCollector

trait PacketCollectorFactory[C <: PacketCollector] {

    def createNew(traffic: TrafficHandler, collectorId: Int): C

}
