package fr.`override`.linkit.api.local.system.security

trait BytesHasher {

    def hashBytes(raw: Array[Byte]): Array[Byte]

    def deHashBytes(hashed: Array[Byte]): Array[Byte]

    val key: String

}

object BytesHasher {
    class Inactive extends BytesHasher {
        override def hashBytes(raw: Array[Byte]): Array[Byte] = raw

        override def deHashBytes(hashed: Array[Byte]): Array[Byte] = hashed

        override val key: String = "default-hasher"
    }

    def inactive: BytesHasher = new Inactive
}