package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.packet.traffic.PacketTraffic

trait PacketCollectorFactory[C <: PacketCollector] {

    final val factory = this //For Java users

    val collectorClass: Class[C]

    def createNew(traffic: PacketTraffic, collectorId: Int): C




    type T <: CollectorBehavior

    implicit def behavioralFactory(behavior: T): PacketCollectorFactory[C] = this

    //an Empty behavior description
    trait CollectorBehavior {
    }
}
