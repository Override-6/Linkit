package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.local.system.AppException

case class ConnectionException(msg: String) extends AppException(msg) {

}
