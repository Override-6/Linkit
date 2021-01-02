package fr.`override`.linkit.api.packet.factory

import fr.`override`.linkit.api.packet.Packet

override.linkit.api.packet.factory

trait PacketFactory[T <: Packet] {

    val packetClass: Class[T]

    def decompose(implicit packet: T): Array[Byte]

    def canTransform(implicit bytes: Array[Byte]): Boolean

    def build(implicit bytes: Array[Byte]): T

}
