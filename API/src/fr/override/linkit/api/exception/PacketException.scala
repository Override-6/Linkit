package fr.`override`.linkit.api.exception

case class PacketException(msg: String) extends RelayException(msg)