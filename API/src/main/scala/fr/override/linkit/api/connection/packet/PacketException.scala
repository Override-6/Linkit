package fr.`override`.linkit.api.connection.packet

class PacketException(msg: String, cause: Throwable = null) extends Exception(msg, cause) {

}

object PacketException {
    def apply(msg: String, cause: Throwable): PacketException = new PacketException(msg, cause)
}
