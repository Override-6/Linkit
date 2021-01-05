package fr.`override`.linkit.api.packet

override.linkit.api.packet

trait PacketFactory[T <: Packet] {

    val packetClass: Class[T]

    def decompose(implicit packet: T): Array[Byte]

    def canTransform(implicit bytes: Array[Byte]): Boolean

    def build(implicit bytes: Array[Byte]): T

    final val factory = this //For Java users

}
