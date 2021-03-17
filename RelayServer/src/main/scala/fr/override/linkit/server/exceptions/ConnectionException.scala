package fr.`override`.linkit.server.exceptions

import fr.`override`.linkit.skull.internal.system.RelayException


case class ConnectionException(msg: String) extends RelayException(msg) {

}
