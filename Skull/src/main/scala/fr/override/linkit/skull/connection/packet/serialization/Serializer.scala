package fr.`override`.linkit.skull.connection.packet.serialization

trait Serializer {

    val signature: Array[Byte]

    def serialize(serializable: Serializable): Array[Byte]

    def isSameSignature(bytes: Array[Byte]): Boolean

    def deserialize(bytes: Array[Byte]): Serializable

    def deserializeAll(bytes: Array[Byte]): Array[Serializable]

}
