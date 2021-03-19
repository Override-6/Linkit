package fr.`override`.linkit.core.connection.packet

case class UnexpectedPacketException(msg: String, cause: Throwable = null) extends Exception(msg, cause){

}
