package fr.`override`.linkit.api.packet


trait PacketFactory[T <: Packet] {

    val packetClass: Class[T]

    val factory: this.type = this //For Java users

    def decompose(translator: PacketTranslator)(implicit packet: T): Array[Byte]

    def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean

    def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): T

}
