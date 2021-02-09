package fr.`override`.linkit.api.packet

trait PacketSerialisationStrategy[P <: Packet] {

    def serialize(packet: P): Array[Byte]

    def deserialize(bytes: Array[Byte]): P

}
