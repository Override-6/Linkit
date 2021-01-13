package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketTraffic

trait PacketCollectorFactory[C <: PacketCollector] {

    final val factory = this //For Java users
    val collectorClass: Class[C]

    def createNew(traffic: PacketTraffic, collectorId: Int): C

}
