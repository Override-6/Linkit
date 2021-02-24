package fr.`override`.linkit.api.system.security

import fr.`override`.linkit.api.exception.RelayException

case class RelaySecurityException(msg: String) extends RelayException(msg){

}
