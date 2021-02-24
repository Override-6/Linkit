package fr.`override`.linkit.api.exception

case class PacketException(msg: String, cause: Throwable = null) extends RelayException(msg, cause)