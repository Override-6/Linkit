package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.packet.Packet

private[sync] case class RMIExceptionString(exceptionString: String) extends Packet {

}
