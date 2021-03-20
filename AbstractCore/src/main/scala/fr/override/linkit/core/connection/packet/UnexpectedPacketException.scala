package fr.`override`.linkit.core.connection.packet

import fr.`override`.linkit.api.connection.packet.PacketException

case class UnexpectedPacketException(msg: String, cause: Throwable = null) extends PacketException(msg, cause){

}
