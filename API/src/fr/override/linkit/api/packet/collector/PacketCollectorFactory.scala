package fr.`override`.linkit.api.packet.collector

import fr.`override`.linkit.api.packet.traffic.PacketSender

trait PacketCollectorFactory[C <: PacketCollector] {

    final val factory = this //For Java users
    val collectorClass: Class[C]

    def createNew(sender: PacketSender, collectorId: Int): C

}
