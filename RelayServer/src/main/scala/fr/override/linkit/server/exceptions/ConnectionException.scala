package fr.`override`.linkit.server.exceptions

import fr.`override`.linkit.api.exception.RelayException


case class ConnectionException(msg: String) extends RelayException(msg) {

}
