package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketWriter

trait PacketCollectorFactory[C <: PacketCollector] {

    final val factory = this //For Java users
    val collectorClass: Class[C]

    def createNew(writer: PacketWriter, collectorId: Int): C

}
