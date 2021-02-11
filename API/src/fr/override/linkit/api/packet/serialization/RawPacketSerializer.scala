package fr.`override`.linkit.api.packet.serialization

class RawPacketSerializer extends PacketSerializer {
    override protected def serializeType(clazz: Class[_]): Array[Byte] = {
        clazz.getName.getBytes ++ ";".getBytes
    }

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    override protected def deserializeType(bytes: Array[Byte]): (Class[_], Int) = {
        val length = bytes.indexOfSlice(";".getBytes)
        val className = new String(bytes.slice(0, length))
        (Class.forName(className), length + 1) //adding the ';' character
    }

    override protected val signature: Array[Byte] = Array(1)
}
