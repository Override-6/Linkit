package fr.overridescala.linkkit.api.`extension`.packet

import fr.overridescala.linkkit.api.packet.Packet

trait PacketFactory[T <: Packet] {

    def decompose(implicit packet: T): Array[Byte]

    def canTransform(implicit bytes: Array[Byte]): Boolean

    def build(implicit bytes: Array[Byte]): T

    val packetClass: Class[T]

}
