package fr.`override`.linkit.core.connection.packet.serialization

object NumberSerializer {
    def serializeLong(value: Long): Array[Byte] = {
        Array[Byte](
            ((value >> 56) & 0xff).toByte,
            ((value >> 48) & 0xff).toByte,
            ((value >> 40) & 0xff).toByte,
            ((value >> 32) & 0xff).toByte,
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def serializeInt(value: Int): Array[Byte] = {
        Array[Byte](
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def serializeShort(value: Short): Array[Byte] = {
        Array[Byte](
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    def deserializeShort(bytes: Array[Byte], index: Int): Short = {
        (((0xff & bytes(index)) << 8) |
                ((0xff & bytes(index + 1)) << 0)).toShort
    }

    def deserializeInt(bytes: Array[Byte], index: Int): Int = {
        (0xff & bytes(index)) << 24 |
                ((0xff & bytes(index + 1)) << 16) |
                ((0xff & bytes(index + 2)) << 8) |
                ((0xff & bytes(index + 3)) << 0)
    }

    def deserializeLong(bytes: Array[Byte], index: Int): Long = {
        //println("Deserializing int in zone " + new String(bytes.slice(index, index + 8)))
        (0xff & bytes(index)) << 52 |
                (0xff & bytes(index + 1)) << 48 |
                (0xff & bytes(index + 2)) << 40 |
                (0xff & bytes(index + 3)) << 32 |
                (0xff & bytes(index + 4)) << 24 |
                ((0xff & bytes(index + 5)) << 16) |
                ((0xff & bytes(index + 6)) << 8) |
                ((0xff & bytes(index + 7)) << 0)
    }

    def convertToShortArray(array: Array[Byte]): Array[Short] = {
        array.grouped(2).map(deserializeShort(_, 0)).toArray
    }

    def convertToIntArray(array: Array[Byte]): Array[Int] = {
        array.grouped(4).map(deserializeInt(_, 0)).toArray
    }

    def convertToLongArray(array: Array[Byte]): Array[Long] = {
        array.grouped(8).map(deserializeLong(_, 0)).toArray
    }

    def convertByteArray(array: Array[Short]): Array[Byte] = {
        array.flatMap(serializeShort)
    }

    def convertByteArray(array: Array[Int]): Array[Byte] = {
        array.flatMap(serializeInt)
    }

    def convertByteArray(array: Array[Long]): Array[Byte] = {
        array.flatMap(serializeLong)
    }

}
