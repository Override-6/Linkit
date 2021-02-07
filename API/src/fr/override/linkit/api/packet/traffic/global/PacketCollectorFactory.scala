package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.packet.traffic.PacketWriter

trait PacketCollectorFactory[C <: PacketCollector] {

    def createNew(writer: PacketWriter): C

}
