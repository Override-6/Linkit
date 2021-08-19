package fr.linkit.engine.connection.cache.obj

import fr.linkit.api.connection.packet.Packet

private[obj] case class RMIExceptionString(exceptionString: String) extends Packet {

}
